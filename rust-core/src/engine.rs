use crate::firewall::Firewall;
use crate::analyzer::{analyze_threat, ThreatType};
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

    pub fn process_request(&mut self, domain: &str, rate: u32, unknown: bool, burst: f32) -> (bool, String) {
        let (score, threat_type) = analyze_threat(rate, unknown, burst);
        let threshold = if self.security_level == 1 { 45 } else { 75 };

        if score > threshold {
            self.firewall.block_domain(domain);
            return (false, format!("{:?}", threat_type));
        }

        (self.firewall.analyze(domain, rate), "Normal".to_string())
    }

    pub fn set_security_level(&mut self, level: u8) {
        self.security_level = level;
    }

    pub fn set_app_rule(&mut self, app_id: &str, blocked: bool) {
        self.firewall.set_app_protection(app_id, blocked);
    }

    pub fn check_health(&self) -> u8 {
        // Mock health check
        100
    }

    pub fn heal(&self) {
        Healer::repair_rules();
    }
}
