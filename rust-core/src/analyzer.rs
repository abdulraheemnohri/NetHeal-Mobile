#[derive(Debug, PartialEq)]
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

    if request_rate > 500 {
        score += 80;
        threat = ThreatType::DDoS;
    } else if request_rate > 200 {
        score += 40;
        threat = ThreatType::Bot;
    }

    if unknown_domain {
        score += 30;
        if threat == ThreatType::Normal { threat = ThreatType::Tracker; }
    }

    if burst_ratio > 15.0 {
        score += 60;
        threat = ThreatType::Malware;
    }

    if score > 100 { score = 100; }
    (score as u8, threat)
}

pub fn risk_score(request_rate: u32, unknown_domain: bool, burst_ratio: f32) -> u8 {
    analyze_threat(request_rate, unknown_domain, burst_ratio).0
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_advanced_analysis() {
        let (score, threat) = analyze_threat(600, false, 1.0);
        assert!(score >= 80);
        assert_eq!(threat, ThreatType::DDoS);

        let (_score, threat) = analyze_threat(50, false, 20.0);
        assert_eq!(threat, ThreatType::Malware);
    }
}
