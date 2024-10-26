use crate::Data;
use reqwest::Client;
use serde::{Deserialize, Serialize};
use serenity::all::Attachment;
use serenity::Error;
use std::env;
use std::ops::Deref;

type PrefixContext<'a> = poise::PrefixContext<'a, Data, Error>;
#[poise::command(prefix_command)]
pub async fn binfiles(ctx: PrefixContext<'_>) -> Result<(), Error> {
    let file_message = &ctx.msg.referenced_message;

    let files: Vec<Attachment> = match file_message {
        None => {
            ctx.say("Reply to a message with an attatchment to create a codebin.")
                .await?;
            return Ok(());
        }
        Some(message) => message.deref().attachments.clone(),
    };
    let service = BinService::LuckoPaste;
    let client = Client::new();
    let mut urls: Vec<String> = Vec::new();

    for file in files {
        let download = file.download().await?;
        let Ok(data) = String::from_utf8(download) else {
            ctx.say("File does not contain valid utf-8!").await?;
            return Ok(());
        };

        match service
            .upload_bin("paste.insprill.net", &data, &client)
            .await
        {
            Ok(url) => urls.push(url),
            Err(_) => {
                ctx.say("Failed to upload attachment to bin!").await?;
            }
        };
    }

    ctx.say(urls.join("\n")).await?;
    Ok(())
}

enum BinService {
    HastebinLegacy,
    LuckoPaste,
    Pastebin,
    SourceBin,
}

impl BinService {
    const fn download_url(&self) -> &'static str {
        match self {
            Self::HastebinLegacy | Self::Pastebin => "https://{}/raw/{}",
            Self::LuckoPaste => "https://{}/data/{}",
            Self::SourceBin => "https://cdn.{}/bins/{}",
        }
    }

    const fn key_pattern(&self) -> &'static str {
        match self {
            Self::HastebinLegacy => "[a-z]+",
            Self::LuckoPaste => "[a-zA-Z0-9]+",
            Self::Pastebin => "[a-zA-Z0-9]{5,10}",
            Self::SourceBin => "[a-zA-Z0-9]{10}",
        }
    }

    fn upload_bin_req(&self, domain: &str, data: &str, client: &Client) -> reqwest::RequestBuilder {
        match self {
            Self::HastebinLegacy => {
                let url = format!("https://{domain}/documents");
                client.post(&url).body(data.to_string())
            }
            Self::LuckoPaste => {
                let url = format!("https://{domain}/data/post");
                client.post(&url).body(data.to_string())
            }
            Self::Pastebin => {
                let api_key = env::var("PASTEBIN_API_KEY").unwrap_or_default();
                let encoded_key = urlencoding::encode(&api_key);
                let encoded_body = urlencoding::encode(data);
                let body = format!(
                    "api_dev_key={encoded_key}&api_option=paste&api_paste_private=1&api_paste_code={encoded_body}"
                );
                let url = format!("https://{domain}/api/api_post.php");
                client.post(&url).body(body).header(
                    "Content-Type",
                    "application/x-www-form-urlencoded; charset=utf-8",
                )
            }
            Self::SourceBin => {
                let files = vec![SourceBinRequestFile {
                    name: String::new(),
                    content: data.to_string(),
                }];
                let req_body = SourceBinRequest { files };
                let url = format!("https//{domain}/api/bins");
                client.post(&url).json(&req_body)
            }
        }
    }
    async fn upload_bin(
        &self,
        domain: &str,
        data: &str,
        client: &Client,
    ) -> Result<String, reqwest::Error> {
        if matches!(self, Self::Pastebin) {
            let req = self.upload_bin_req(domain, data, client);
            let res = req.send().await?;
            let text = res.text().await?;
            Ok(text)
        } else {
            let req = self.upload_bin_req(domain, data, client);
            let res = req.send().await?;
            let res_json: GenericBinResponse = res.json().await?;
            Ok(format!("https://{}/{}", domain, res_json.key))
        }
    }
}

#[derive(Serialize)]
struct SourceBinRequest {
    files: Vec<SourceBinRequestFile>,
}

#[derive(Serialize)]
struct SourceBinRequestFile {
    name: String,
    content: String,
}

#[derive(Deserialize)]
struct GenericBinResponse {
    key: String,
}
