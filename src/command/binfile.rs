use crate::Data;
use reqwest::Client;
use serde::{Deserialize, Serialize};
use serenity::all::Attachment;
use serenity::Error;
use std::env;

type PrefixContext<'a> = poise::PrefixContext<'a, Data, Error>;
#[poise::command(prefix_command)]
pub async fn binfiles(ctx: PrefixContext<'_>) -> Result<(), Error> {
    let file_message = &ctx.msg.referenced_message;
    let typing = ctx.serenity_context.http.start_typing(ctx.channel_id());

    let files: Vec<Attachment> = match file_message {
        None => {
            ctx.say("Reply to a message with an attatchment to create a codebin.")
                .await?;
            return Ok(());
        }
        Some(message) => message.attachments.clone(),
    };
    let service = BinService::LUCKO_PASTE;
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
    typing.stop();
    ctx.say(urls.join("\n")).await?;
    Ok(())
}
#[allow(non_camel_case_types)]
#[derive(Deserialize, Clone, Debug, Eq, PartialEq, Hash)]
pub enum BinService {
    HASTEBIN_LEGACY,
    LUCKO_PASTE,
    PASTEBIN,
    SOURCE_BIN,
}

impl BinService {
    #[allow(dead_code)]
    const fn download_url(&self) -> &'static str {
        match self {
            Self::HASTEBIN_LEGACY | Self::PASTEBIN => "https://{}/raw/{}",
            Self::LUCKO_PASTE => "https://{}/data/{}",
            Self::SOURCE_BIN => "https://cdn.{}/bins/{}",
        }
    }

    #[allow(dead_code)]
    const fn key_pattern(&self) -> &'static str {
        match self {
            Self::HASTEBIN_LEGACY => "[a-z]+",
            Self::LUCKO_PASTE => "[a-zA-Z0-9]+",
            Self::PASTEBIN => "[a-zA-Z0-9]{5,10}",
            Self::SOURCE_BIN => "[a-zA-Z0-9]{10}",
        }
    }

    fn upload_bin_req(&self, domain: &str, data: &str, client: &Client) -> reqwest::RequestBuilder {
        match self {
            Self::HASTEBIN_LEGACY => {
                let url = format!("https://{domain}/documents");
                client.post(&url).body(data.to_string())
            }
            Self::LUCKO_PASTE => {
                let url = format!("https://{domain}/data/post");
                client.post(&url).body(data.to_string())
            }
            Self::PASTEBIN => {
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
            Self::SOURCE_BIN => {
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
        if matches!(self, Self::PASTEBIN) {
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
