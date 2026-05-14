use std::collections::HashMap;

pub enum ThreatType { Normal, Bot, DDoS, DGA, Exfiltration }

pub struct ThreatReport {
    pub risk_score: u8,
    pub threat_type: ThreatType,
}

pub fn calculate_entropy(s: &str) -> f32 {
    let mut frequencies = HashMap::new();
    for c in s.chars() { *frequencies.entry(c).or_insert(0) += 1; }
    let len = s.len() as f32;
    if len == 0.0 { return 0.0; }
    frequencies.values().map(|&count| {
        let p = count as f32 / len;
        -p * p.log2()
    }).sum()
}

pub fn analyze_threat(_request_rate: u32, _burst_ratio: f32, domain: Option<&str>) -> ThreatReport {
    let mut score: u8 = 0;
    let mut t_type = ThreatType::Normal;
    if let Some(d) = domain {
        let entropy = calculate_entropy(d);
        if entropy > 4.1 && d.len() > 10 {
            score = 85;
            t_type = ThreatType::DGA;
        } else if d.contains("analytics") || d.contains("tracker") || d.contains("telemetry") {
            score = 65;
            t_type = ThreatType::Exfiltration;
        }
    }
    ThreatReport { risk_score: score.min(100), threat_type: t_type }
}
