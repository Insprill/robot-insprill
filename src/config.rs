use crate::command::binfile::BinService;
use serde::Deserialize;
use std::collections::HashMap;

#[derive(Deserialize, Debug, Clone)]
pub struct Config {
    pub builtins: Builtins,
    pub codebin: CodeBin,
}

#[derive(Deserialize, Debug, Clone)]
pub struct Builtins {
    pub clear: bool,
    pub binfiles: BinfilesConfig
}

#[derive(Deserialize, Debug, Clone)]
pub struct BinfilesConfig {
    pub enabled: bool,
    pub upload: BinService,
}
#[derive(Deserialize, Debug, Clone)]
pub struct CodeBin {
    pub services: HashMap<BinService, Vec<String>>, // <Service, Domains>
}