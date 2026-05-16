use std::collections::{HashSet, HashMap};
use crate::analyzer::{detect_anomalies, analyze_behavior};

pub struct AppEntry {
    pub state: u8,
    pub bytes_sent: u64,
    pub bytes_recv: u64,
    pub history_sizes: Vec<usize>,
    pub history_times: Vec<u64>,
    pub history_ports: Vec<u16>,
}

pub struct Firewall {
    whitelist_ips: HashSet<String>,
    whitelist_domains: HashSet<String>,
    blacklist_ips: HashSet<String>,
    blacklist_domains: HashSet<String>,
    geo_blocks: HashSet<String>,
    port_blocks: HashSet<u16>,
    apps: HashMap<String, AppEntry>,
    security_level: u8,
    performance_mode: bool,
    jules_active: bool,
    blocked_count: u64,
    scanned_count: u64,
    observed_domains: HashSet<String>,
}

impl Firewall {
    pub fn new() -> Self {
        Firewall {
            whitelist_ips: HashSet::new(),
            whitelist_domains: HashSet::new(),
            blacklist_ips: HashSet::new(),
            blacklist_domains: HashSet::new(),
            geo_blocks: HashSet::new(),
            port_blocks: HashSet::new(),
            apps: HashMap::new(),
            security_level: 0,
            performance_mode: false,
            jules_active: false,
            blocked_count: 0,
            scanned_count: 0,
            observed_domains: HashSet::new(),
        }
    }

    pub fn analyze_packet(&mut self, data: &[u8], dst_ip: &str, _protocol: u8, app_id: Option<&str>, domain: Option<&str>) -> bool {
        self.scanned_count += 1;
        if let Some(d) = domain { self.observed_domains.insert(d.to_string()); }

        if let Some(id) = app_id {
            let entry = self.apps.entry(id.to_string()).or_insert(AppEntry {
                state: 0, bytes_sent: 0, bytes_recv: 0,
                history_sizes: Vec::new(), history_times: Vec::new(), history_ports: Vec::new(),
            });
            if entry.state == 2 { self.blocked_count += 1; return false; }

            entry.history_sizes.push(data.len());
            if entry.history_sizes.len() > 20 { entry.history_sizes.remove(0); }

            if let Some((score, _pattern)) = analyze_behavior(&entry.history_sizes, &entry.history_times, &entry.history_ports) {
                if score > 80 && self.security_level >= 3 {
                    entry.state = 2;
                    self.blocked_count += 1;
                    return false;
                }
            }
        }

        if self.blacklist_ips.contains(dst_ip) { self.blocked_count += 1; return false; }
        if let Some(d) = domain { if self.blacklist_domains.contains(d) { self.blocked_count += 1; return false; } }

        if self.security_level >= 2 {
            if let Some((score, _)) = detect_anomalies(data) {
                if score > 90 { self.blocked_count += 1; return false; }
            }
        }

        true
    }

    pub fn get_app_state(&self, app_id: &str) -> u8 { self.apps.get(app_id).map(|e| e.state).unwrap_or(0) }
    pub fn set_app_state(&mut self, app_id: &str, state: u8) {
        let entry = self.apps.entry(app_id.to_string()).or_insert(AppEntry {
            state, bytes_sent: 0, bytes_recv: 0,
            history_sizes: Vec::new(), history_times: Vec::new(), history_ports: Vec::new(),
        });
        entry.state = state;
    }

    pub fn whitelist_ip(&mut self, ip: &str) { self.whitelist_ips.insert(ip.to_string()); }
    pub fn unwhitelist_ip(&mut self, ip: &str) { self.whitelist_ips.remove(ip); }
    pub fn whitelist_domain(&mut self, d: &str) { self.whitelist_domains.insert(d.to_string()); }
    pub fn unwhitelist_domain(&mut self, d: &str) { self.whitelist_domains.remove(d); }
    pub fn block_ip(&mut self, ip: &str) { self.blacklist_ips.insert(ip.to_string()); }
    pub fn unblock_ip(&mut self, ip: &str) { self.blacklist_ips.remove(ip); }
    pub fn block_domain(&mut self, d: &str) { self.blacklist_domains.insert(d.to_string()); }
    pub fn unblock_domain(&mut self, d: &str) { self.blacklist_domains.remove(d); }

    pub fn add_geo_block(&mut self, c: String) { self.geo_blocks.insert(c); }
    pub fn remove_geo_block(&mut self, c: String) { self.geo_blocks.remove(&c); }
    pub fn add_port_block(&mut self, p: u16) { self.port_blocks.insert(p); }
    pub fn remove_port_block(&mut self, p: u16) { self.port_blocks.remove(&p); }

    pub fn set_security_level(&mut self, lvl: u8) { self.security_level = lvl; }
    pub fn set_performance_mode(&mut self, enabled: bool) { self.performance_mode = enabled; }
    pub fn set_jules_active(&mut self, enabled: bool) { self.jules_active = enabled; }

    pub fn update_ai_risk(&mut self, _target: String, _risk: u8) {}
    pub fn record_traffic(&mut self, app_id: &str, bytes: u64, sent: bool) {
        if let Some(entry) = self.apps.get_mut(app_id) {
            if sent { entry.bytes_sent += bytes; } else { entry.bytes_recv += bytes; }
        }
    }

    pub fn get_stats(&self) -> (u64, u64) { (self.scanned_count, self.blocked_count) }
    pub fn get_app_usage_json(&self) -> String {
        let mut json = String::from("{");
        for (i, (id, entry)) in self.apps.iter().enumerate() {
            if i > 0 { json.push(','); }
            json.push_str(&format!("\"{}\":{{\"p\":{},\"r\":{}}}", id, entry.bytes_sent, entry.bytes_recv));
        }
        json.push('}');
        json
    }
    pub fn reset_stats(&mut self) { self.scanned_count = 0; self.blocked_count = 0; }
    pub fn reset_rules(&mut self) {
        self.blacklist_ips.clear();
        self.blacklist_domains.clear();
    }
}
