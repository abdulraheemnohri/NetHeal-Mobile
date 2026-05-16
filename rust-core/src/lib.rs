use crate::engine::Engine;
use jni::objects::{JByteArray, JClass, JString};
use jni::sys::{jboolean, jbyteArray, jint, jlong};
use jni::JNIEnv;
use once_cell::sync::Lazy;
use std::sync::Mutex;

mod advanced;
mod analyzer;
mod booster;
mod engine;
mod firewall;
mod healer;
mod packet;

static ENGINE: Lazy<Mutex<Engine>> = Lazy::new(|| Mutex::new(Engine::new()));

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_startEngine(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    1 // Success
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_stopEngine(_env: JNIEnv, _class: JClass) {
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_getStats<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass,
) -> JString<'local> {
    let engine = ENGINE.lock().unwrap();
    let stats = format!(
        "Scanned: {}, Blocked: {}",
        engine.get_scanned_count(),
        engine.get_blocked_count()
    );
    env.new_string(stats).unwrap()
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
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setAppRule(
    mut env: JNIEnv,
    _class: JClass,
    app_id: JString,
    rule: jint,
) {
    let app_id_str: String = env
        .get_string(&app_id)
        .expect("Couldn't get java string!")
        .into();
    let mut engine = ENGINE.lock().unwrap();
    engine.set_app_rule(&app_id_str, rule as u8);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_addWhitelist(
    mut env: JNIEnv,
    _class: JClass,
    target: JString,
    is_domain: jboolean,
) {
    let target_str: String = env
        .get_string(&target)
        .expect("Couldn't get java string!")
        .into();
    let mut engine = ENGINE.lock().unwrap();
    engine.add_whitelist(&target_str, is_domain != 0);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_removeWhitelist(
    mut env: JNIEnv,
    _class: JClass,
    target: JString,
    is_domain: jboolean,
) {
    let target_str: String = env
        .get_string(&target)
        .expect("Couldn't get java string!")
        .into();
    let mut engine = ENGINE.lock().unwrap();
    engine.remove_whitelist(&target_str, is_domain != 0);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_addBlacklist(
    mut env: JNIEnv,
    _class: JClass,
    target: JString,
    is_domain: jboolean,
) {
    let target_str: String = env
        .get_string(&target)
        .expect("Couldn't get java string!")
        .into();
    let mut engine = ENGINE.lock().unwrap();
    engine.add_blacklist(&target_str, is_domain != 0);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_removeBlacklist(
    mut env: JNIEnv,
    _class: JClass,
    target: JString,
    is_domain: jboolean,
) {
    let target_str: String = env
        .get_string(&target)
        .expect("Couldn't get java string!")
        .into();
    let mut engine = ENGINE.lock().unwrap();
    engine.remove_blacklist(&target_str, is_domain != 0);
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
pub extern "system" fn Java_com_netheal_bridge_RustBridge_addGeoBlock(
    mut env: JNIEnv,
    _class: JClass,
    country: JString,
) {
    let country_str: String = env
        .get_string(&country)
        .expect("Couldn't get java string!")
        .into();
    let mut engine = ENGINE.lock().unwrap();
    engine.add_geo_block(country_str);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_removeGeoBlock(
    mut env: JNIEnv,
    _class: JClass,
    country: JString,
) {
    let country_str: String = env
        .get_string(&country)
        .expect("Couldn't get java string!")
        .into();
    let mut engine = ENGINE.lock().unwrap();
    engine.remove_geo_block(country_str);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setBooster(
    _env: JNIEnv,
    _class: JClass,
    active: jboolean,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.set_booster_active(active != 0);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setBoosterActive(
    _env: JNIEnv,
    _class: JClass,
    active: jboolean,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.set_booster_active(active != 0);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setMultipath(
    _env: JNIEnv,
    _class: JClass,
    active: jboolean,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.set_multipath_active(active != 0);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setMultipathActive(
    _env: JNIEnv,
    _class: JClass,
    active: jboolean,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.set_multipath_active(active != 0);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setShapingMode(
    _env: JNIEnv,
    _class: JClass,
    mode: jint,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.set_shaping_mode(mode as i32);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setPerformanceMode(
    _env: JNIEnv,
    _class: JClass,
    active: jboolean,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.set_performance_mode(active != 0);
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
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setBatterySafeguard(
    _env: JNIEnv,
    _class: JClass,
    active: jboolean,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.set_battery_safeguard(active != 0);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setAiActive(
    _env: JNIEnv,
    _class: JClass,
    active: jboolean,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.set_ai_active(active != 0);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setNeuralShield(
    _env: JNIEnv,
    _class: JClass,
    active: jboolean,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.set_neural_shield(active != 0);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_updateAiRisk(
    mut env: JNIEnv,
    _class: JClass,
    package_name: JString,
    score: jint,
) {
    let pkg: String = env
        .get_string(&package_name)
        .expect("Couldn't get java string!")
        .into();
    let mut engine = ENGINE.lock().unwrap();
    engine.update_ai_risk(pkg, score as u8);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_applyDpiScript(
    mut env: JNIEnv,
    _class: JClass,
    pattern: JString,
    action: JString,
) {
    let p_str: String = env
        .get_string(&pattern)
        .expect("Couldn't get java string!")
        .into();
    let a_str: String = env
        .get_string(&action)
        .expect("Couldn't get java string!")
        .into();
    let mut engine = ENGINE.lock().unwrap();
    engine.apply_dpi_script(p_str, a_str);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setHoneypotMode(
    _env: JNIEnv,
    _class: JClass,
    active: jboolean,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.set_honeypot_mode(active != 0);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setFingerprintMask(
    _env: JNIEnv,
    _class: JClass,
    mask_type: jint,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.set_fingerprint_mask(mask_type as i32);
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
pub extern "system" fn Java_com_netheal_bridge_RustBridge_getBlockedCount(
    _env: JNIEnv,
    _class: JClass,
) -> jlong {
    let engine = ENGINE.lock().unwrap();
    engine.get_blocked_count() as jlong
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
pub extern "system" fn Java_com_netheal_bridge_RustBridge_getAnalytics(
    mut env: JNIEnv,
    _class: JClass,
) -> jbyteArray {
    let engine = ENGINE.lock().unwrap();
    let analytics = engine.get_analytics_json();
    env.byte_array_from_slice(analytics.as_bytes())
        .unwrap()
        .into_raw()
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
pub extern "system" fn Java_com_netheal_bridge_RustBridge_clearLogs(_env: JNIEnv, _class: JClass) {
    let mut engine = ENGINE.lock().unwrap();
    engine.reset_stats();
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setUpstreamDns(
    mut env: JNIEnv,
    _class: JClass,
    dns: JString,
) {
    let dns_str: String = env
        .get_string(&dns)
        .expect("Couldn't get java string!")
        .into();
    let mut engine = ENGINE.lock().unwrap();
    engine.set_upstream_dns(&dns_str);
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_handlePacket(
    mut env: JNIEnv,
    _class: JClass,
    packet: JByteArray,
) -> jboolean {
    let mut data_u8 = env.convert_byte_array(&packet).unwrap();
    let mut engine = ENGINE.lock().unwrap();
    let allowed = engine.handle_packet(&mut data_u8, None);
    if allowed {
        let data_i8: Vec<i8> = data_u8.into_iter().map(|b| b as i8).collect();
        env.set_byte_array_region(&packet, 0, &data_i8).unwrap();
    }
    if allowed {
        1
    } else {
        0
    }
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_heal(
    _env: JNIEnv,
    _class: JClass,
    _packet: JByteArray,
) -> jbyteArray {
    let mut engine = ENGINE.lock().unwrap();
    engine.heal();
    std::ptr::null_mut()
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_getAppRule(
    mut env: JNIEnv,
    _class: JClass,
    app_id: JString,
) -> jint {
    let app_id_str: String = env
        .get_string(&app_id)
        .expect("Couldn't get java string!")
        .into();
    let engine = ENGINE.lock().unwrap();
    engine.get_app_rule(&app_id_str) as jint
}

#[no_mangle]
pub extern "system" fn Java_com_netheal_bridge_RustBridge_setObfuscation(
    _env: jni::JNIEnv,
    _class: jni::objects::JClass,
    active: jni::sys::jboolean,
) {
    let mut engine = ENGINE.lock().unwrap();
    engine.set_obfuscation_active(active != 0);
}
