use crate::firewall::Firewall;
use crate::healer::Healer;
use crate::packet::parse_v4;

pub struct Engine {
    firewall: Firewall,
    security_level: u8,
}

impl Engine {
    pub fn new() -> Self {
        Engine {
            firewall: Firewall::new(),
            security_level: 0,
        }
    }

    pub fn handle_packet(&mut self, data: &[u8], app_id: Option<&str>) -> bool {
        if let Some(info) = parse_v4(data) {
            self.firewall.analyze_packet(&info.dst_ip, app_id)
        } else {
            true
        }
    }

    pub fn set_security_level(&mut self, level: u8) {
        self.security_level = level;
        self.firewall.set_military_mode(level == 2);
    }

    pub fn set_app_rule(&mut self, app_id: &str, blocked: bool) {
        self.firewall.set_app_protection(app_id, blocked);
    }

    pub fn add_whitelist(&mut self, ip: &str) {
        self.firewall.whitelist_ip(ip);
    }

    pub fn remove_whitelist(&mut self, ip: &str) {
        self.firewall.unwhitelist_ip(ip);
    }

    pub fn add_blacklist(&mut self, ip: &str) {
        self.firewall.block_ip(ip);
    }

    pub fn remove_blacklist(&mut self, ip: &str) {
        self.firewall.unblock_ip(ip);
    }

    pub fn get_blocked_count(&self) -> u64 {
        self.firewall.get_stats().1
    }

    pub fn get_scanned_count(&self) -> u64 {
        self.firewall.get_stats().0
    }

    pub fn check_health(&self) -> u8 {
        // Higher security level means "healthier" (more protected)
        if self.security_level == 2 { 100 } else if self.security_level == 1 { 90 } else { 80 }
    }

    pub fn heal(&mut self) {
        Healer::repair_rules();
        self.firewall.reset_rules();
    }
}
