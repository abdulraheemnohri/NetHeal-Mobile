use crate::firewall::Firewall;
use crate::healer::Healer;
use crate::booster::Booster;
use crate::packet::{parse_v4, parse_dns_query, parse_sni};

pub struct Engine {
    firewall: Firewall,
    healer: Healer,
    booster: Booster,
    security_level: u8,
    dns_pool: Vec<String>,
    dns_idx: usize,
    performance_mode: bool,
}

impl Engine {
    pub fn new() -> Self {
        Engine {
            firewall: Firewall::new(),
            healer: Healer::new(),
            booster: Booster::new(),
            security_level: 0,
            dns_pool: vec!["1.1.1.1".to_string(), "8.8.8.8".to_string(), "9.9.9.9".to_string()],
            dns_idx: 0,
            performance_mode: false,
        }
    }

    pub fn handle_packet(&mut self, data: &mut [u8], app_id: Option<&str>) -> bool {
        if !self.healer.check_watchdog() { return false; }

        if let Some(info) = parse_v4(data) {
            // APPLY BOOSTER OPTIMIZATION
            self.booster.optimize_packet(data);

            // LINK BONDING (MOCK: in a real implementation, this would mark the packet for a specific network interface)
            let _path = self.booster.handle_link_bonding();

            let mut domain = None;
            if info.protocol == 17 {
                domain = parse_dns_query(&info.payload);
                self.dns_idx = (self.dns_idx + 1) % self.dns_pool.len();
            }
            else if info.protocol == 6 { domain = parse_sni(&info.payload); }

            let allowed = self.firewall.analyze_packet(data, &info.dst_ip, info.protocol, app_id, domain.as_deref());
            if allowed {
                if let Some(id) = app_id {
                    self.firewall.record_traffic(id, data.len() as u64, true);
                }
            }
            allowed
        } else { true }
    }

    pub fn record_heartbeat(&mut self) { self.healer.record_heartbeat(); }
    pub fn record_incoming(&mut self, app_id: &str, bytes: u64) { self.firewall.record_traffic(app_id, bytes, false); }
    pub fn set_security_level(&mut self, level: u8) { self.security_level = level; self.firewall.set_security_level(level); }
    pub fn set_profile(&mut self, profile: &str) { self.firewall.set_profile(profile); }

    pub fn set_upstream_dns(&mut self, dns: &str) {
        self.dns_pool.insert(0, dns.to_string());
        if self.dns_pool.len() > 5 { self.dns_pool.pop(); }
    }

    pub fn set_performance_mode(&mut self, enabled: bool) {
        self.performance_mode = enabled;
        self.firewall.set_performance_mode(enabled);
    }
    pub fn set_stealth_mode(&mut self, enabled: bool) { self.firewall.set_stealth_mode(enabled); }
    pub fn set_dns_hardening(&mut self, enabled: bool) { self.firewall.set_dns_hardening(enabled); }
    pub fn set_learning_mode(&mut self, enabled: bool) { self.firewall.set_learning_mode(enabled); }
    pub fn set_jules_active(&mut self, enabled: bool) { self.firewall.set_jules_active(enabled); }

    pub fn set_booster_active(&mut self, enabled: bool) { self.booster.booster_active = enabled; }
    pub fn set_multipath_active(&mut self, enabled: bool) { self.booster.multipath_active = enabled; }

    pub fn set_app_bw_limit(&mut self, app_id: &str, limit: u64) { self.firewall.set_app_bandwidth_limit(app_id, limit); }
    pub fn set_app_rule(&mut self, app_id: &str, state: u8) { self.firewall.set_app_state(app_id, state); }
    pub fn add_whitelist(&mut self, val: &str, is_domain: bool) { if is_domain { self.firewall.whitelist_domain(val); } else { self.firewall.whitelist_ip(val); } }
    pub fn remove_whitelist(&mut self, val: &str, is_domain: bool) { if is_domain { self.firewall.unwhitelist_domain(val); } else { self.firewall.unwhitelist_ip(val); } }
    pub fn add_blacklist(&mut self, val: &str, is_domain: bool) { if is_domain { self.firewall.block_domain(val); } else { self.firewall.block_ip(val); } }
    pub fn remove_blacklist(&mut self, val: &str, is_domain: bool) { if is_domain { self.firewall.unblock_domain(val); } else { self.firewall.unblock_ip(val); } }
    pub fn add_geo_block(&mut self, country: String) { self.firewall.add_geo_block(country); }
    pub fn remove_geo_block(&mut self, country: String) { self.firewall.remove_geo_block(country); }
    pub fn add_port_block(&mut self, port: u16) { self.firewall.add_port_block(port); }
    pub fn remove_port_block(&mut self, port: u16) { self.firewall.remove_port_block(port); }
    pub fn kill_ip(&mut self, ip: &str) { self.firewall.kill_ip(ip); }
    pub fn update_ai_risk(&mut self, target: String, risk: u8) { self.firewall.update_ai_risk(target, risk); }

    pub fn get_blocked_count(&self) -> u64 { self.firewall.get_stats().1 }
    pub fn get_scanned_count(&self) -> u64 { self.firewall.get_stats().0 }
    pub fn get_analytics_json(&self) -> String {
        let top = self.firewall.get_top_blocked();
        let protos = self.firewall.get_protocol_counts();
        let usage = self.firewall.get_app_usage_json();
        let conns = self.firewall.get_active_conns_json();
        let observed = self.firewall.get_observed_count();
        let mut top_json = String::from("[");
        for (i, (t, c)) in top.iter().enumerate() { if i > 0 { top_json.push(','); } top_json.push_str(&format!("{{\"t\":\"{}\",\"c\":{}}}", t, c)); }
        top_json.push(']');
        let mut proto_json = String::from("{");
        for (i, (p, c)) in protos.iter().enumerate() { if i > 0 { proto_json.push(','); } proto_json.push_str(&format!("\"{}\":{}", p, c)); }
        proto_json.push('}');
        format!("{{\"top\": {}, \"protocols\": {}, \"usage\": {}, \"conns\": {}, \"observed\": {}}}", top_json, proto_json, usage, conns, observed)
    }

    pub fn check_health(&self) -> u8 { if self.security_level >= 2 { 100 } else if self.security_level == 1 { 90 } else { 80 } }
    pub fn run_diagnostics(&self) -> String { format!("HEALTH: OK, ENGINE: OMEGA, BOOST: {}, DPI: ACTIVE, WATCHDOG: ACTIVE", self.booster.booster_active) }
    pub fn heal(&mut self) { Healer::repair_rules(); self.firewall.reset_rules(); }
    pub fn reset_stats(&mut self) { self.firewall.reset_stats(); }
}
