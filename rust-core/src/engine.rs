use crate::firewall::Firewall;
use crate::healer::Healer;
use crate::booster::Booster;
use crate::packet::{parse_v4, parse_dns_query, parse_sni};
use crate::advanced::AdvancedEngine;
use crate::analyzer::{analyze_behavior, calculate_entropy};

pub struct Engine {
    firewall: Firewall,
    healer: Healer,
    booster: Booster,
    security_level: u8,
    dns_pool: Vec<String>,
    dns_idx: usize,
    performance_mode: bool,
    neural_shield: bool,
    shaping_mode: i32,
    buffer_size: u32,
    battery_safeguard: bool,
    stealth_mode: bool,
    advanced: AdvancedEngine,
    packet_sizes: Vec<usize>,
    inter_times: Vec<u64>,
    ports: Vec<u16>,
    dns_query_log: Vec<String>,
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
            neural_shield: false,
            shaping_mode: 0,
            buffer_size: 16384,
            battery_safeguard: true,
            stealth_mode: false,
            advanced: AdvancedEngine::new(),
            packet_sizes: Vec::new(),
            inter_times: Vec::new(),
            ports: Vec::new(),
            dns_query_log: Vec::new(),
        }
    }

    pub fn handle_packet(&mut self, data: &mut [u8], app_id: Option<&str>) -> bool {
        if !self.healer.check_watchdog() { return false; }

        if let Some(info) = parse_v4(data) {
            self.booster.optimize_packet(data);
            self.advanced.apply_deception(data);
            if self.stealth_mode { self.booster.apply_privacy_shield(data); }

            // DNA ALGORITHM: Detect DNS Tunneling
            if info.protocol == 17 {
                if let Some(domain) = parse_dns_query(&info.payload) {
                    self.dns_query_log.push(domain.clone());
                    if self.dns_query_log.len() > 10 { self.dns_query_log.remove(0); }

                    let entropy = calculate_entropy(&domain);
                    if entropy > 4.5 {
                        self.firewall.update_ai_risk("DNS_TUNNEL_PROBABLE".to_string(), 90);
                    }
                }
            }

            self.packet_sizes.push(data.len());
            self.ports.push(info.dst_port);
            if self.packet_sizes.len() > 20 { self.packet_sizes.remove(0); }
            if self.ports.len() > 20 { self.ports.remove(0); }

            if let Some((risk, pattern)) = analyze_behavior(&self.packet_sizes, &self.inter_times, &self.ports) {
                 if risk > 80 {
                     self.firewall.update_ai_risk(pattern, risk);
                 }
            }

            let allowed = self.firewall.analyze_packet(data, &info.dst_ip, info.protocol, app_id, None);
            allowed
        } else { true }
    }

    pub fn get_system_security_report(&self) -> String {
        // Android-specific interrogator mock
        // In a real environment, we'd use libc to read system properties or JNI callbacks
        format!("BOOTLOADER: LOCKED, ROOT: NOT_DETECTED, SELINUX: ENFORCING, KERNEL: OMEGA_MAX_V4, UPTIME: {}s",
                std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).unwrap().as_secs() % 10000)
    }

    pub fn record_heartbeat(&mut self) { self.healer.record_heartbeat(); }
    pub fn set_security_level(&mut self, level: u8) { self.security_level = level; self.firewall.set_security_level(level); }
    pub fn set_upstream_dns(&mut self, dns: &str) {
        self.dns_pool.insert(0, dns.to_string());
    }

    pub fn set_performance_mode(&mut self, enabled: bool) { self.performance_mode = enabled; self.firewall.set_performance_mode(enabled); }
    pub fn set_stealth_mode(&mut self, enabled: bool) { self.stealth_mode = enabled; }
    pub fn set_jules_active(&mut self, enabled: bool) { self.firewall.set_jules_active(enabled); }
    pub fn set_neural_shield(&mut self, enabled: bool) { self.neural_shield = enabled; }
    pub fn set_shaping_mode(&mut self, mode: i32) { self.shaping_mode = mode; }
    pub fn set_buffer_size(&mut self, size: u32) { self.buffer_size = size; }
    pub fn set_battery_safeguard(&mut self, enabled: bool) { self.battery_safeguard = enabled; }
    pub fn set_obfuscation_active(&mut self, enabled: bool) { self.booster.obfuscation_active = enabled; }
    pub fn set_booster_active(&mut self, enabled: bool) { self.booster.booster_active = enabled; }
    pub fn set_multipath_active(&mut self, enabled: bool) { self.booster.multipath_active = enabled; }
    pub fn set_ghost_mode(&mut self, enabled: bool) { self.advanced.ghost_mode_active = enabled; }

    pub fn get_app_rule(&self, app_id: &str) -> u8 { self.firewall.get_app_state(app_id) }
    pub fn set_app_rule(&mut self, app_id: &str, state: u8) { self.firewall.set_app_state(app_id, state); }
    pub fn add_whitelist(&mut self, val: &str, is_domain: bool) { if is_domain { self.firewall.whitelist_domain(val); } else { self.firewall.whitelist_ip(val); } }
    pub fn remove_whitelist(&mut self, val: &str, is_domain: bool) { if is_domain { self.firewall.unwhitelist_domain(val); } else { self.firewall.unwhitelist_ip(val); } }
    pub fn add_blacklist(&mut self, val: &str, is_domain: bool) { if is_domain { self.firewall.block_domain(val); } else { self.firewall.block_ip(val); } }
    pub fn remove_blacklist(&mut self, val: &str, is_domain: bool) { if is_domain { self.firewall.unblock_domain(val); } else { self.firewall.unblock_ip(val); } }
    pub fn add_geo_block(&mut self, country: String) { self.firewall.add_geo_block(country); }
    pub fn remove_geo_block(&mut self, country: String) { self.firewall.remove_geo_block(country); }
    pub fn add_port_block(&mut self, port: u16) { self.firewall.add_port_block(port); }
    pub fn remove_port_block(&mut self, port: u16) { self.firewall.remove_port_block(port); }
    pub fn update_ai_risk(&mut self, target: String, risk: u8) { self.firewall.update_ai_risk(target, risk); }

    pub fn get_blocked_count(&self) -> u64 { self.firewall.get_stats().1 }
    pub fn get_scanned_count(&self) -> u64 { self.firewall.get_stats().0 }
    pub fn get_analytics_json(&self) -> String {
        let usage = self.firewall.get_app_usage_json();
        format!("{{\"usage\": {}}}", usage)
    }

    pub fn check_health(&self) -> u8 { if self.security_level >= 2 { 100 } else if self.security_level == 1 { 90 } else { 80 } }
    pub fn heal(&mut self) { Healer::repair_rules(); self.firewall.reset_rules(); }
    pub fn reset_stats(&mut self) { self.firewall.reset_stats(); }

    pub fn set_honeypot_mode(&mut self, enabled: bool) { self.advanced.honeypot_active = enabled; }
    pub fn set_fingerprint_mask(&mut self, mask_type: i32) {
        let os = match mask_type {
            1 => "Windows 11",
            2 => "Linux Kernel",
            3 => "iOS 17",
            _ => "None",
        };
        self.advanced.fake_os_fingerprint = os.to_string();
    }
    pub fn apply_dpi_script(&mut self, pattern: String, action: String) { self.advanced.add_dpi_script(pattern, action); }
}
