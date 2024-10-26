use crate::command::binfile::BinService;
use serde::Deserialize;
use std::collections::HashMap;

pub struct Config {
    pub builtins: Builtins,
    pub code_bin_config: CodeBinConfig,
}

#[derive(Deserialize, Debug, Clone)]
pub struct Builtins {
    pub clear: bool,
}

#[derive(Deserialize, Debug, Clone)]
pub struct CodeBinConfig {
    pub upload: BinService,
    pub services: HashMap<BinService, Vec<String>>, // <Service, Domains>
}
