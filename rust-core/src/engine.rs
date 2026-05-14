use crate::firewall::Firewall;
use crate::analyzer::analyze_threat;
use crate::healer::Healer;

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

    pub fn process_request(&mut self, target: &str, is_ip: bool, rate: u32, burst: f32, app_id: Option<&str>) -> (bool, u8) {
        let (score, _) = analyze_threat(rate, false, burst);
        let threshold = if self.security_level == 1 { 45 } else { 75 };

        if score > threshold {
            if is_ip { self.firewall.block_ip(target); } else { self.firewall.block_domain(target); }
            return (false, score);
        }

        let allowed = self.firewall.analyze_connection(target, is_ip, rate, app_id);
        (allowed, score)
    }

    pub fn set_security_level(&mut self, level: u8) {
        self.security_level = level;
    }

    pub fn set_app_rule(&mut self, app_id: &str, blocked: bool) {
        self.firewall.set_app_protection(app_id, blocked);
    }

    pub fn add_whitelist(&mut self, domain: &str) {
        self.firewall.add_to_whitelist(domain);
    }

    pub fn remove_whitelist(&mut self, domain: &str) {
        self.firewall.remove_from_whitelist(domain);
    }

    pub fn add_blacklist(&mut self, target: &str) {
        let is_ip = target.chars().next().map_or(false, |c| c.is_ascii_digit());
        if is_ip {
            self.firewall.block_ip(target);
        } else {
            self.firewall.block_domain(target);
        }
    }

    pub fn remove_blacklist(&mut self, target: &str) {
        let is_ip = target.chars().next().map_or(false, |c| c.is_ascii_digit());
        if is_ip {
            self.firewall.remove_ip_block(target);
        } else {
            self.firewall.remove_domain_block(target);
        }
    }

    pub fn get_blocked_count(&self) -> u64 {
        self.firewall.get_stats().1
    }

    pub fn get_scanned_count(&self) -> u64 {
        self.firewall.get_stats().0
    }

    pub fn check_health(&self) -> u8 {
        100
    }

    pub fn heal(&mut self) {
        Healer::repair_rules();
        self.firewall.reset_rules();
    }
}
