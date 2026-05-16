pub struct PacketInfo {
    pub src_ip: String,
    pub dst_ip: String,
    pub src_port: u16,
    pub dst_port: u16,
    pub protocol: u8,
    pub payload: Vec<u8>,
}

pub fn parse_v4(data: &[u8]) -> Option<PacketInfo> {
    if data.len() < 20 { return None; }
    if (data[0] >> 4) != 4 { return None; }
    let ihl = (data[0] & 0x0F) as usize * 4;
    if data.len() < ihl { return None; }
    let protocol = data[9];
    let src_ip = format!("{}.{}.{}.{}", data[12], data[13], data[14], data[15]);
    let dst_ip = format!("{}.{}.{}.{}", data[16], data[17], data[18], data[19]);

    let mut src_port = 0;
    let mut dst_port = 0;
    if data.len() >= ihl + 4 {
        src_port = ((data[ihl] as u16) << 8) | data[ihl+1] as u16;
        dst_port = ((data[ihl+2] as u16) << 8) | data[ihl+3] as u16;
    }

    let payload = data[ihl..].to_vec();
    Some(PacketInfo { src_ip, dst_ip, src_port, dst_port, protocol, payload })
}

pub fn parse_dns_query(payload: &[u8]) -> Option<String> {
    if payload.len() < 20 { return None; }
    let dns_data = &payload[8..];
    if dns_data.len() < 12 { return None; }
    let q_count = ((dns_data[4] as u16) << 8) | dns_data[5] as u16;
    if q_count == 0 { return None; }
    let mut pos = 12;
    let mut domain = String::new();
    while pos < dns_data.len() {
        let len = dns_data[pos] as usize;
        if len == 0 { break; }
        if pos + 1 + len > dns_data.len() { return None; }
        if !domain.is_empty() { domain.push('.'); }
        domain.push_str(&String::from_utf8_lossy(&dns_data[pos + 1..pos + 1 + len]));
        pos += 1 + len;
    }
    if domain.is_empty() { None } else { Some(domain) }
}

pub fn parse_sni(payload: &[u8]) -> Option<String> {
    if payload.len() < 40 { return None; }
    let tcp_off = ((payload[12] & 0xF0) >> 4) as usize * 4;
    if payload.len() < tcp_off + 10 { return None; }
    let tls_data = &payload[tcp_off..];
    if tls_data[0] != 0x16 { return None; }
    if tls_data.len() < 6 || tls_data[5] != 0x01 { return None; }
    let mut i = 43;
    if tls_data.len() < i + 10 { return None; }
    while i < tls_data.len().saturating_sub(5) {
        if tls_data[i] == 0x00 && tls_data[i+1] == 0x00 {
            let ext_len = ((tls_data[i+2] as u16) << 8) | tls_data[i+3] as u16;
            if tls_data.len() < i + 4 + ext_len as usize { return None; }
            let sni_data = &tls_data[i+4..i+4+ext_len as usize];
            if sni_data.len() > 5 && sni_data[2] == 0x00 {
                let name_len = ((sni_data[3] as u16) << 8) | sni_data[4] as u16;
                if sni_data.len() >= 5 + name_len as usize {
                    return Some(String::from_utf8_lossy(&sni_data[5..5+name_len as usize]).to_string());
                }
            }
        }
        i += 1;
    }
    None
}
