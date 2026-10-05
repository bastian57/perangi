# Fase 0 Research Report: How open-source apps implement DNS filtering over Android VpnService

**Projects studied:** DNS66 (julian-klode/dns66), NetGuard (M66B/NetGuard), RethinkDNS (celzero/rethink-app), Blokada (blokadaorg/blokada). All findings verified against current source on GitHub.
**Date:** 2026-10-05

## 0. TL;DR — what to copy for PERANGI

DNS66 is the closest architectural model for us: a **DNS-only VPN** (not a full tunnel), pure Java/Kotlin, ~6 small files. NetGuard shows the full-tunnel + native-code alternative. RethinkDNS shows the modern DoH-upstream + firewall approach. Key takeaway: route **only DNS traffic** into the TUN (not all traffic) — simpler, faster, less battery.

---

## 1. TUN interface setup (VpnService.Builder parameters)

### DNS66 — `AdVpnThread.java`, method `configure()` (the gold standard for us)
- **Address:** tries documentation-reserved prefixes first — `192.0.2.1/24`, `198.51.100.1/24`, `203.0.113.1/24` (falls back to `192.168.50.1/24`). IPv6: `2001:db8::/120`.
- **The clever trick — virtual DNS aliases:** for each real upstream DNS server, it creates a fake alias (`192.0.2.2`, `192.0.2.3`, …) and calls `builder.addDnsServer(alias)` + `builder.addRoute(alias, 32)`. Android then sends **all DNS queries** to these fake IPs, which land in our TUN. Inside the app, the last octet minus 2 maps back to the real upstream (`translateDestinationAdress()`).
- **Routes:** only the `/32` DNS aliases — **no `0.0.0.0/0` route**. Non-DNS traffic never enters the VPN. This is the single biggest battery/perf win.
- **Other builder calls:** `setBlocking(true)`, `allowBypass()`, `allowFamily(AF_INET)` + `allowFamily(AF_INET6)`, `setMetered(false)` (API 29+), `setSession("DNS66")`, `setConfigureIntent(...)`, then `establish()`.
- **Upstream socket protection:** the `DatagramSocket` used to forward queries calls `vpnService.protect(socket)` so forwarded DNS bypasses the VPN (no loop).

### NetGuard — `ServiceSinkhole.java`, method `getBuilder()`
- Full-tunnel firewall: `addAddress("10.1.10.1", 32)` (+ `fd00:1:fd00:1:fd00:1:fd00:1/128` for v6), `addRoute("0.0.0.0", 0)` + `addRoute("2000::", 3)`.
- MTU comes from native code: `builder.setMtu(jni_get_mtu())`.
- Notably **excludes Android Private DNS IPs from routes** so the OS-level private-DNS setting doesn't break or bypass the tunnel.
- `addAllowedApplication` / `addDisallowedApplication` for per-app filtering; `setUnderlyingNetworks()`.

### RethinkDNS — `BraveVPNService.kt` (~6600 lines)
- Constants: `IPV4_TEMPLATE = "10.111.222.%d"` (/24), `IPV6_TEMPLATE = "fd66:f83a:c650::%d"` (/120, RFC 4193 ULA), `VPN_INTERFACE_MTU = 1500`.
- Two modes: **DNS-only** (routes only the fake DNS IPs, like DNS66) or full tunnel (`0.0.0.0/0`).
- Fake DNS IPs via `LanIp.DNS.make(...)` helpers; `addDnsServer()` + matching `addRoute()` for the fake DNS.
- Actual packet I/O is in **Go** (`firestack` module, gomobile AAR — a hard fork of Jigsaw's outline-go-tun2socks); Kotlin handles policy, blocklist decisions, and UI.

### Foreground service handling (all)
- All run as foreground services with a persistent notification (`startForeground(NOTIFICATION_ID, ...)`), `START_STICKY`, and a `RECEIVE_BOOT_COMPLETED` receiver for auto-start (DNS66: `BootComplete.java` → `checkStartVpnOnBoot()`; only restarts if the user had it active and `VpnService.prepare()` returns null i.e. already authorized).
- DNS66 restarts the VPN thread on connectivity change; RethinkDNS has a full `ConnectionMonitor` + `VpnBuilderPolicy` (AUTO/SENSITIVE/RELAXED/FIXED) controlling when the TUN is rebuilt.

---

## 2. DNS packet parsing: custom vs library

### DNS66 — libraries, not hand-rolled (recommended for us)
- **IP/UDP parsing:** `org.pcap4j` (`IpSelector.newPacket()`, `UdpPacket`, `IpV4Packet`/`IpV6Packet` builders). pcap4j is MIT-licensed — we can use it directly.
- **DNS parsing:** `org.xbill.DNS` (**dnsjava**, BSD-licensed — we can use it directly): `new Message(dnsRawData)`, `dnsMsg.getQuestion().getName()`.
- **File:** `vpn/DnsPacketProxy.java` — `handleDnsRequest(byte[])` parses IP→UDP→DNS, extracts the query name; `handleDnsResponse(IpPacket, byte[])` rebuilds the IP/UDP headers with swapped src/dst and writes back to the TUN via `queueDeviceWrite()`.
- **Blocked response construction:** sets the QR flag, `Rcode.NOERROR`, and appends an **SOA record with 5-second TTL** (`NEGATIVE_CACHE_SOA_RECORD`) purely for negative caching, then serializes with `dnsMsg.toWire()`. (NetGuard instead uses a configurable rcode, default **NXDOMAIN=3**. Rethink's cloud resolver returns **0.0.0.0**. All three are valid; NXDOMAIN is the most standard "blocked" signal.)

### NetGuard — native C
- Packet parsing/forwarding is all in JNI (`app/src/main/jni/netguard.c`): `jni_run(context, tun_fd, fwd53, rcode)`. Java side only passes `mapHostsBlocked`/`mapMalware` maps down. Fast, but JNI complexity we don't need for MVP.

### RethinkDNS — Go
- DNS transport in Go (`firestack`); per-query policy callback into Kotlin: `onQuery(origin, uid, fqdn, qtype) → DNSOpts`, handled by `tunnel/TunDnsManager.kt` (`handleOnQuery`). Blocklist matching happens in Kotlin (`service/DomainRulesManager.kt`, `service/RethinkBlocklistManager.kt`).

**Recommendation:** copy DNS66's approach — pcap4j + dnsjava, both permissively licensed, no JNI, no Go toolchain.

---

## 3. Blocklist matching: data structures

- **DNS66** — `db/RuleDatabase.java`: `AtomicReference<HashSet<String>> blockedHosts`; `isBlocked(host)` is a single `HashSet.contains()` — O(1), lock-free reads, atomic swap on reload. Hosts files are parsed from classic `127.0.0.1 domain` / `0.0.0.0 domain` format. **Limitation: exact-match only, no subdomain/wildcard matching** — `ads.example.com` does NOT match a rule for `example.com`. (NetGuard documents the same limitation: "wildcards are not supported due to performance and battery usage reasons.")
- **NetGuard** — `Map<String, Boolean> mapHostsBlocked` (+ `mapMalware`), populated by `prepareHostsBlocked()` from the imported hosts file, consulted in native code.
- **RethinkDNS** — blocklists live in Room/SQLite (`database/` package) with `DomainRulesManager`; notably they **follow CNAME and HTTPS/SVCB redirects and match those against the blocklist** to defeat DNS cloaking — worth copying later.

**Recommendation for PERANGI:** start with DNS66's `HashSet` + atomic-swap pattern (simple, fast, thread-safe). Add **suffix matching** (walk `a.b.judol.com` → check `b.judol.com` → `judol.com`) since gambling operators rotate subdomains — it's still O(labels) hash lookups, cheap. Add CNAME-chasing in v2 like Rethink.

---

## 4. Upstream forwarding

- **DNS66:** plain **UDP** via `DatagramSocket` to the device's current DNS servers (read from `ConnectivityManager.getLinkProperties().getDnsServers()`) or user-configured ones. Request/response correlation via `WospList` — a bounded (max 1024) time-limited (10s) map of pending queries. No DoT/DoH support.
- **NetGuard:** full tunnel — DNS is just forwarded like any other traffic (or answered locally for blocked hosts). DoT/DoH from browsers bypasses its inspection entirely (documented).
- **RethinkDNS:** user-chosen **DoH / DoT / DNSCrypt / Oblivious DoH** upstreams, handled in Go. This is the modern standard.
- **Blokada 6:** WireGuard cloud tunnel; DNS resolved by their cloud backend. (`EngineService.kt` — `decideDoh()` currently returns false; on-device filtering was the Blokada 5 "Libre" approach, no longer in this tree.)

**Recommendation:** MVP = DNS66-style plain UDP forwarding to a fixed upstream (1.1.1.1 / 8.8.8.8). v2 = DoT/DoH upstream like Rethink (better privacy, harder for ISPs to tamper with).

---

## 5. DoH-bypass countermeasures (browsers using their own Secure DNS)

Honest summary: **none of them fully solve this** — it's the known hard problem:

- **NetGuard** (`ADBLOCKING.md`): explicitly tells users to **disable** Android Private DNS *and* each browser's Secure DNS (Chrome/Firefox DoH/DoT), because encrypted DNS "cannot be seen" — it flows as ordinary TCP 443/853. No automatic countermeasure.
- **DNS66:** no countermeasure (2016-era design, predates mainstream DoH).
- **RethinkDNS:** two partial mitigations — (a) a **"Universal firewall rule: block when DNS is bypassed"** (kills app network access if it detects the app bypassing the VPN DNS), and (b) since it can run as a **full tunnel**, DoH traffic to known resolver IPs can be firewall-blocked per-app.
- General technique available to us (used by Rethink-style firewalls): **block port 853 (DoT)** and **blocklist known DoH provider domains/IPs** (`dns.google`, `cloudflare-dns.com`, `dns.quad9.net`, `doh.opendns.com`, …) so browsers fail closed and fall back to system DNS — which we control. Note Chrome also has a fallback: if the DoH probe fails it silently uses system DNS, which is exactly what we want.

**Recommendation for PERANGI:** v1 = onboarding screen that walks the user through disabling Chrome's "Use secure DNS" + Android Private DNS (like NetGuard's docs), PLUS ship a built-in blocklist of ~20 well-known DoH/DoT endpoints so bypass attempts fail closed into our filter. v2 = Rethink-style "block app when DNS bypass detected" using per-UID connection tracking.

---

## 6. Battery/performance tricks worth copying

1. **DNS-only routing** (DNS66, Rethink DNS-mode): only `/32` routes for fake DNS IPs enter the TUN. Everything else bypasses the VPN entirely — near-zero overhead when idle.
2. **Blocking TUN** (`setBlocking(true)`) + `Os.poll()` on the TUN fd instead of busy read loops (DNS66's `doOne()` polls the TUN fd + all pending DNS sockets in one syscall).
3. **Bounded pending-query table** (DNS66 `WospList`: 1024 entries, 10s TTL) — prevents socket/fd leaks under DNS floods.
4. **No wildcards/regex** in the hot path (NetGuard, Rethink both document this) — hash lookups only.
5. **Atomic-swap blocklist reload** (DNS66 `AtomicReference<HashSet>`) — readers never block; reload builds the new set off-thread then swaps.
6. **Watchdog via poll timeout**, not a second thread (DNS66 `VpnWatchdog`).
7. **Negative-caching SOA with tiny TTL (5s)** on blocked answers — stops clients hammering the same blocked domain without poisoning their cache long-term.
8. **Native/Go packet path** (NetGuard/Rethink) is faster but unnecessary for DNS-only filtering at our scale.

---

## 7. Licenses — what it means for PERANGI

| Project | License | Implication for us |
|---|---|---|
| DNS66 | **GPL-3.0** | Strong copyleft. We may **study and reimplement ideas** in our own words, but must **not copy code verbatim** into PERANGI unless PERANGI itself is GPL-3.0. Clean-room reimplementation is the safe path. |
| NetGuard | **GPL-3.0** | Same as above. |
| RethinkDNS | **Apache-2.0** | Permissive — code *could* be reused with attribution, but it's a massive Go+Kotlin codebase; realistically we take **ideas only**. |
| Blokada | **MPL-2.0** | File-level copyleft — an MPL file stays MPL, but can coexist with proprietary code. Current tree is v6 (cloud WireGuard), less relevant to us. |
| pcap4j (lib) | MIT | ✅ Can use directly as a dependency. |
| dnsjava (lib) | BSD 3-clause | ✅ Can use directly as a dependency. |

**Bottom line:** the *safest and most practical* path is a clean-room Kotlin implementation modeled on **DNS66's architecture** (DNS-only VPN + virtual DNS aliases + HashSet blocklist + UDP forwarding), using the permissively-licensed **pcap4j** and **dnsjava** libraries directly. No GPL code gets copied; the design knowledge is free.

### Also critical — Play Store policy warning (from NetGuard's ADBLOCKING.md + Rethink's README)
Google **does not allow on-device ad/DNS-blocking apps on the Play Store** — NetGuard's Play version has the hosts-blocking feature stripped (GitHub version only). RethinkDNS's README states on-device blocklists "aren't possible" for the Play flavour, so they moved blocking to their cloud resolver. **For PERANGI this means:** plan distribution via our own website / GitHub Releases APK from day one; treat Play Store listing as a bonus with a feature-reduced build, not the primary channel.
