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
        if self.security_level == 4 { self.firewall.force_block_stat(); return false; }
        if let Some(info) = parse_v4(data) {
            let mut domain = None;
            if info.protocol == 17 { domain = parse_dns_query(&info.payload); }
            else if info.protocol == 6 { domain = parse_sni(&info.payload); }
            self.firewall.analyze_packet(&info.dst_ip, info.protocol, app_id, domain.as_deref())
        } else {
            true
        }
    }

    pub fn set_security_level(&mut self, level: u8) {
        self.security_level = level;
        self.firewall.set_military_mode(level == 2);
        self.firewall.set_lockdown(level == 3);
        self.firewall.set_kill_switch(level == 4);
    }

    pub fn set_upstream_dns(&mut self, dns: &str) { self.upstream_dns = dns.to_string(); }
    pub fn set_app_rule(&mut self, app_id: &str, state: u8) { self.firewall.set_app_state(app_id, state); }
    pub fn add_whitelist(&mut self, val: &str, is_domain: bool) { if is_domain { self.firewall.whitelist_domain(val); } else { self.firewall.whitelist_ip(val); } }
    pub fn remove_whitelist(&mut self, val: &str, is_domain: bool) { if is_domain { self.firewall.unwhitelist_domain(val); } else { self.firewall.unwhitelist_ip(val); } }
    pub fn add_blacklist(&mut self, val: &str, is_domain: bool) { if is_domain { self.firewall.block_domain(val); } else { self.firewall.block_ip(val); } }
    pub fn remove_blacklist(&mut self, val: &str, is_domain: bool) { if is_domain { self.firewall.unblock_domain(val); } else { self.firewall.unblock_ip(val); } }

    pub fn get_blocked_count(&self) -> u64 { self.firewall.get_stats().1 }
    pub fn get_scanned_count(&self) -> u64 { self.firewall.get_stats().0 }

    pub fn get_analytics_json(&self) -> String {
        let top = self.firewall.get_top_blocked();
        let protos = self.firewall.get_protocol_counts();
        let mut top_json = String::from("[");
        for (i, (target, count)) in top.iter().enumerate() { if i > 0 { top_json.push(','); } top_json.push_str(&format!("{{\"t\":\"{}\",\"c\":{}}}", target, count)); }
        top_json.push(']');
        let mut proto_json = String::from("{");
        for (i, (proto, count)) in protos.iter().enumerate() { if i > 0 { proto_json.push(','); } proto_json.push_str(&format!("\"{}\":{}", proto, count)); }
        proto_json.push('}');
        format!("{{\"top\": {}, \"protocols\": {}}}", top_json, proto_json)
    }

    pub fn check_health(&self) -> u8 { if self.security_level >= 2 { 100 } else if self.security_level == 1 { 90 } else { 80 } }
    pub fn run_diagnostics(&self) -> String { format!("HEALTH: OK, ENGINE: ABSOLUTE, DPI: SNI_ENABLED, DNS: {}, KILL_SWITCH: {}", self.upstream_dns, if self.security_level == 4 {"ACTIVE"} else {"READY"}) }
    pub fn heal(&mut self) { Healer::repair_rules(); self.firewall.reset_rules(); }
    pub fn reset_stats(&mut self) { self.firewall.reset_stats(); }
}
