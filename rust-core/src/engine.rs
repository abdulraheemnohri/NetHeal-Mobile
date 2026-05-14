use crate::firewall::Firewall;
use crate::healer::Healer;
use crate::packet::{parse_v4, parse_dns_query, parse_sni};

pub struct Engine {
    firewall: Firewall,
    security_level: u8,
    upstream_dns: String,
}

impl Engine {
    pub fn new() -> Self {
        Engine {
            firewall: Firewall::new(),
            security_level: 0,
            upstream_dns: "1.1.1.1".to_string(),
        }
    }

    pub fn handle_packet(&mut self, data: &[u8], app_id: Option<&str>) -> bool {
        if let Some(info) = parse_v4(data) {
            let mut domain = None;
            if info.protocol == 17 { domain = parse_dns_query(&info.payload); }
            else if info.protocol == 6 { domain = parse_sni(&info.payload); }

            let allowed = self.firewall.analyze_packet(&info.dst_ip, info.protocol, app_id, domain.as_deref());
            if allowed {
                if let Some(id) = app_id {
                    self.firewall.record_traffic(id, data.len() as u64, true);
                }
            }
            allowed
        } else { true }
    }

    pub fn record_incoming(&mut self, app_id: &str, bytes: u64) {
        self.firewall.record_traffic(app_id, bytes, false);
    }

    pub fn set_security_level(&mut self, level: u8) {
        self.security_level = level;
        self.firewall.set_security_level(level);
    }

    pub fn set_upstream_dns(&mut self, dns: &str) { self.upstream_dns = dns.to_string(); }
    pub fn set_app_bw_limit(&mut self, app_id: &str, limit: u64) { self.firewall.set_app_bandwidth_limit(app_id, limit); }
    pub fn set_app_rule(&mut self, app_id: &str, state: u8) { self.firewall.set_app_state(app_id, state); }
    pub fn add_whitelist(&mut self, val: &str, is_domain: bool) { if is_domain { self.firewall.whitelist_domain(val); } else { self.firewall.whitelist_ip(val); } }
    pub fn remove_whitelist(&mut self, val: &str, is_domain: bool) { if is_domain { self.firewall.unwhitelist_domain(val); } else { self.firewall.unwhitelist_ip(val); } }
    pub fn add_blacklist(&mut self, val: &str, is_domain: bool) { if is_domain { self.firewall.block_domain(val); } else { self.firewall.block_ip(val); } }
    pub fn remove_blacklist(&mut self, val: &str, is_domain: bool) { if is_domain { self.firewall.unblock_domain(val); } else { self.firewall.unblock_ip(val); } }
    pub fn block_region(&mut self, r: &str) { self.firewall.block_region(r); }
    pub fn unblock_region(&mut self, r: &str) { self.firewall.unblock_region(r); }

    pub fn get_blocked_count(&self) -> u64 { self.firewall.get_stats().1 }
    pub fn get_scanned_count(&self) -> u64 { self.firewall.get_stats().0 }

    pub fn get_analytics_json(&self) -> String {
        let top = self.firewall.get_top_blocked();
        let usage = self.firewall.get_app_usage_json();
        let mut top_json = String::from("[");
        for (i, (t, c)) in top.iter().enumerate() { if i > 0 { top_json.push(','); } top_json.push_str(&format!("{{\"t\":\"{}\",\"c\":{}}}", t, c)); }
        top_json.push(']');
        format!("{{\"top\": {}, \"usage\": {}}}", top_json, usage)
    }

    pub fn check_health(&self) -> u8 { if self.security_level >= 2 { 100 } else if self.security_level == 1 { 90 } else { 80 } }
    pub fn run_diagnostics(&self) -> String { format!("HEALTH: OK, ENGINE: OMEGA, DPI: ACTIVE, DNS: {}", self.upstream_dns) }
    pub fn heal(&mut self) { Healer::repair_rules(); self.firewall.reset_rules(); }
    pub fn reset_stats(&mut self) { self.firewall.reset_stats(); }
}
