use std::collections::HashMap;

#[derive(Debug, Clone, Copy)]
pub enum ThreatType {
    Normal,
    Bot,
    DDoS,
    DGA,
    Exfiltration,
    IDS,
    SynFlood,
    Malformed,
    CryptoMining,
    Spyware,
    Tracking,
    InsecureHTTP
}

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

pub fn analyze_threat(request_rate: u32, burst_ratio: f32, domain: Option<&str>) -> ThreatReport {
    let mut score: u8 = 0;
    let mut t_type = ThreatType::Normal;

    if let Some(d) = domain {
        let entropy = calculate_entropy(d);
        if entropy > 4.1 && d.len() > 12 {
            score = 90;
            t_type = ThreatType::DGA;
        } else if d.contains("analytics") || d.contains("tracker") || d.contains("telemetry") {
            score = 65;
            t_type = ThreatType::Tracking;
        } else if d.contains("pool.") || d.contains("mining") || d.contains("hash") {
            score = 95;
            t_type = ThreatType::CryptoMining;
        } else if d.contains("spy") || d.contains("hook") || d.contains("keylog") {
            score = 100;
            t_type = ThreatType::Spyware;
        }
    }

    if request_rate > 500 {
        score = score.max(85);
        t_type = ThreatType::DDoS;
    } else if burst_ratio > 10.0 {
        score = score.max(75);
        t_type = ThreatType::Bot;
    }

    ThreatReport { risk_score: score.min(100), threat_type: t_type }
}

pub fn detect_anomalies(data: &[u8]) -> Option<(u8, ThreatType)> {
    if data.len() < 20 { return Some((90, ThreatType::Malformed)); }

    let version = data[0] >> 4;
    if version != 4 && version != 6 {
        return Some((95, ThreatType::Malformed));
    }

    // Detect insecure HTTP (Port 80)
    // IP Offset 9: Protocol (6=TCP, 17=UDP)
    // TCP header starts at IHV*4. Dest port is bytes 2-3 of TCP header.
    let ihl = (data[0] & 0x0F) as usize * 4;
    if data.len() > ihl + 4 && data[9] == 6 {
        let d_port = ((data[ihl+2] as u16) << 8) | data[ihl+3] as u16;
        if d_port == 80 { return Some((40, ThreatType::InsecureHTTP)); }
    }

    for i in 0..data.len().saturating_sub(4) {
        if &data[i..i+4] == [0x58, 0x50, 0x33, 0x4F] {
            return Some((100, ThreatType::IDS));
        }
    }

    if data.len() > 1500 { return Some((70, ThreatType::Malformed)); }

    None
}
