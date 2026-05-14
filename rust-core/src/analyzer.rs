pub enum ThreatType {
    Normal,
    Bot,
    Malware,
    DDoS,
    Tracker,
    Suspicious,
}

pub struct ThreatReport {
    pub risk_score: u8,
    pub threat_type: ThreatType,
}

pub fn analyze_threat(request_rate: u32, burst_ratio: f32, is_unknown_domain: bool) -> ThreatReport {
    let mut score: u8 = 0;

    if request_rate > 500 {
        score += 80;
    } else if request_rate > 100 {
        score += 40;
    }

    if burst_ratio > 3.0 {
        score += 30;
    }

    if is_unknown_domain {
        score += 20;
    }

    let threat_type = if score >= 90 {
        ThreatType::DDoS
    } else if score >= 70 {
        ThreatType::Malware
    } else if score >= 50 {
        ThreatType::Bot
    } else if score >= 30 {
        ThreatType::Suspicious
    } else {
        ThreatType::Normal
    };

    ThreatReport {
        risk_score: score.min(100),
        threat_type,
    }
}
