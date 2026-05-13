#[derive(Debug, PartialEq, Clone)]
pub enum ThreatType {
    Normal,
    Bot,
    Malware,
    Tracker,
    DDoS,
}

pub fn analyze_threat(request_rate: u32, unknown_domain: bool, burst_ratio: f32) -> (u8, ThreatType) {
    let mut score = 0;
    let mut threat = ThreatType::Normal;

    if request_rate > 1000 {
        score += 95;
        threat = ThreatType::DDoS;
    } else if request_rate > 300 {
        score += 50;
        threat = ThreatType::Bot;
    }

    if unknown_domain {
        score += 35;
        if threat == ThreatType::Normal { threat = ThreatType::Tracker; }
    }

    if burst_ratio > 20.0 {
        score += 70;
        threat = ThreatType::Malware;
    }

    if score > 100 { score = 100; }
    (score as u8, threat)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_threat_levels() {
        let (score, threat) = analyze_threat(1500, false, 1.0);
        assert_eq!(threat, ThreatType::DDoS);
        assert_eq!(score, 95);
    }
}
