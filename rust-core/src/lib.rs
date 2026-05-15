use jni::objects::{JClass, JString, JByteArray};
use jni::sys::{jboolean, jint, jlong, jbyteArray};
use jni::JNIEnv;
use std::sync::Mutex;
use once_cell::sync::Lazy;

mod booster;
mod engine;
mod firewall;
mod analyzer;
mod healer;
mod packet;

use crate::engine::Engine;

static ENGINE: Lazy<Mutex<Engine>> = Lazy::new(|| Mutex::new(Engine::new()));

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_handlePacket(
    mut env: JNIEnv,
    _class: JClass,
    packet: jbyteArray,
) -> jboolean {
    let packet_array = unsafe { JByteArray::from_raw(packet) };
    let mut data = env.convert_byte_array(&packet_array).unwrap_or_default();
    let mut engine = ENGINE.lock().unwrap();
    let allowed = engine.handle_packet(&mut data, None);

    if allowed {
        let i8_data: Vec<i8> = data.into_iter().map(|b| b as i8).collect();
        env.set_byte_array_region(&packet_array, 0, &i8_data).unwrap();
    }

    if allowed { 1 } else { 0 }
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_handlePacketWithApp(
    mut env: JNIEnv,
    _class: JClass,
    packet: jbyteArray,
    app_id: JString,
) -> jboolean {
    let app_id_str: String = env.get_string(&app_id).expect("Couldn't get java string!").into();
    let packet_array = unsafe { JByteArray::from_raw(packet) };
    let mut data = env.convert_byte_array(&packet_array).unwrap_or_default();
    let mut engine = ENGINE.lock().unwrap();
    let allowed = engine.handle_packet(&mut data, Some(&app_id_str));

    if allowed {
        let i8_data: Vec<i8> = data.into_iter().map(|b| b as i8).collect();
        env.set_byte_array_region(&packet_array, 0, &i8_data).unwrap();
    }

    if allowed { 1 } else { 0 }
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_recordHeartbeat(
    _env: JNIEnv,
    _class: JClass,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.record_heartbeat();
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_recordIncoming(
    mut env: JNIEnv,
    _class: JClass,
    app_id: JString,
    bytes: jlong,
) {
    let app_id_str: String = env.get_string(&app_id).expect("Couldn't get java string!").into();
    let mut engine = ENGINE.lock().unwrap();
    engine.record_incoming(&app_id_str, bytes as u64);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setAppRule(
    mut env: JNIEnv,
    _class: JClass,
    app_id: JString,
    state: jint,
) {
    let app_id: String = env.get_string(&app_id).expect("Couldn't get java string!").into();
    let mut engine = ENGINE.lock().unwrap();
    engine.set_app_rule(&app_id, state as u8);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setAppBwLimit(
    mut env: JNIEnv,
    _class: JClass,
    app_id: JString,
    limit: jlong,
) {
    let app_id: String = env.get_string(&app_id).expect("Couldn't get java string!").into();
    let mut engine = ENGINE.lock().unwrap();
    engine.set_app_bw_limit(&app_id, limit as u64);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_addWhitelist(
    mut env: JNIEnv,
    _class: JClass,
    val: JString,
    is_domain: jboolean,
) {
    let val: String = env.get_string(&val).expect("Couldn't get java string!").into();
    let mut engine = ENGINE.lock().unwrap();
    engine.add_whitelist(&val, is_domain != 0);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_removeWhitelist(
    mut env: JNIEnv,
    _class: JClass,
    val: JString,
    is_domain: jboolean,
) {
    let val: String = env.get_string(&val).expect("Couldn't get java string!").into();
    let mut engine = ENGINE.lock().unwrap();
    engine.remove_whitelist(&val, is_domain != 0);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_addBlacklist(
    mut env: JNIEnv,
    _class: JClass,
    ip_or_domain: JString,
    is_domain: jboolean,
) {
    let val: String = env.get_string(&ip_or_domain).expect("Couldn't get java string!").into();
    let mut engine = ENGINE.lock().unwrap();
    engine.add_blacklist(&val, is_domain != 0);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_removeBlacklist(
    mut env: JNIEnv,
    _class: JClass,
    ip_or_domain: JString,
    is_domain: jboolean,
) {
    let val: String = env.get_string(&ip_or_domain).expect("Couldn't get java string!").into();
    let mut engine = ENGINE.lock().unwrap();
    engine.remove_blacklist(&val, is_domain != 0);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_addGeoBlock(
    mut env: JNIEnv,
    _class: JClass,
    country: JString,
) {
    let country: String = env.get_string(&country).expect("Couldn't get java string!").into();
    let mut engine = ENGINE.lock().unwrap();
    engine.add_geo_block(country);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_removeGeoBlock(
    mut env: JNIEnv,
    _class: JClass,
    country: JString,
) {
    let country: String = env.get_string(&country).expect("Couldn't get java string!").into();
    let mut engine = ENGINE.lock().unwrap();
    engine.remove_geo_block(country);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_addPortBlock(
    _env: JNIEnv,
    _class: JClass,
    port: jint,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.add_port_block(port as u16);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_removePortBlock(
    _env: JNIEnv,
    _class: JClass,
    port: jint,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.remove_port_block(port as u16);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_killIp(
    mut env: JNIEnv,
    _class: JClass,
    ip: JString,
) {
    let ip: String = env.get_string(&ip).expect("Couldn't get java string!").into();
    let mut engine = ENGINE.lock().unwrap();
    engine.kill_ip(&ip);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_updateAiRisk(
    mut env: JNIEnv,
    _class: JClass,
    target: JString,
    risk: jint,
) {
    let target: String = env.get_string(&target).expect("Couldn't get java string!").into();
    let mut engine = ENGINE.lock().unwrap();
    engine.update_ai_risk(target, risk as u8);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_heal(
    _env: JNIEnv,
    _class: JClass,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.heal();
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_getSecurityScore(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    let engine = ENGINE.lock().unwrap();
    engine.check_health() as jint
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setSecurityLevel(
    _env: JNIEnv,
    _class: JClass,
    level: jint,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.set_security_level(level as u8);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setProfile(
    mut env: JNIEnv,
    _class: JClass,
    profile: JString,
) {
    let profile: String = env.get_string(&profile).expect("Couldn't get java string!").into();
    let mut engine = ENGINE.lock().unwrap();
    engine.set_profile(&profile);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setUpstreamDns(
    mut env: JNIEnv,
    _class: JClass,
    dns: JString,
) {
    let dns: String = env.get_string(&dns).expect("Couldn't get java string!").into();
    let mut engine = ENGINE.lock().unwrap();
    engine.set_upstream_dns(&dns);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setPerformanceMode(
    _env: JNIEnv,
    _class: JClass,
    enabled: jboolean,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.set_performance_mode(enabled != 0);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setStealthMode(
    _env: JNIEnv,
    _class: JClass,
    enabled: jboolean,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.set_stealth_mode(enabled != 0);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setDnsHardening(
    _env: JNIEnv,
    _class: JClass,
    enabled: jboolean,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.set_dns_hardening(enabled != 0);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setLearningMode(
    _env: JNIEnv,
    _class: JClass,
    enabled: jboolean,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.set_learning_mode(enabled != 0);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setJulesActive(
    _env: JNIEnv,
    _class: JClass,
    enabled: jboolean,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.set_jules_active(enabled != 0);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_getBlockedCount(
    _env: JNIEnv,
    _class: JClass,
) -> jlong {
    let engine = ENGINE.lock().unwrap();
    engine.get_blocked_count() as jlong
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_getScannedCount(
    _env: JNIEnv,
    _class: JClass,
) -> jlong {
    let engine = ENGINE.lock().unwrap();
    engine.get_scanned_count() as jlong
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_resetStats(
    _env: JNIEnv,
    _class: JClass,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.reset_stats();
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_getAnalytics(
    mut env: JNIEnv,
    _class: JClass,
) -> jbyteArray {
    let engine = ENGINE.lock().unwrap();
    let analytics = engine.get_analytics_json();
    let array = env.byte_array_from_slice(analytics.as_bytes()).unwrap();
    array.as_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_runDiagnostics(
    mut env: JNIEnv,
    _class: JClass,
) -> jbyteArray {
    let engine = ENGINE.lock().unwrap();
    let diag = engine.run_diagnostics();
    let array = env.byte_array_from_slice(diag.as_bytes()).unwrap();
    array.as_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setBoosterActive(
    _env: JNIEnv,
    _class: JClass,
    enabled: jboolean,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.set_booster_active(enabled != 0);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setMultipathActive(
    _env: JNIEnv,
    _class: JClass,
    enabled: jboolean,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.set_multipath_active(enabled != 0);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setNeuralShield(
    _env: JNIEnv,
    _class: JClass,
    enabled: jboolean,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.set_neural_shield(enabled != 0);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setBufferSize(
    _env: JNIEnv,
    _class: JClass,
    size: jint,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.set_buffer_size(size as u32);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setShapingMode(
    _env: JNIEnv,
    _class: JClass,
    enabled: jboolean,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.set_shaping_mode(enabled != 0);
}
