import type { DemoConfigResponse } from "@/types"

export async function getDemoConfig(): Promise<DemoConfigResponse> {
  return {
    defaultLocale: "zh-CN",
    locales: ["zh-CN", "en"],
    markets: ["AmazonUS"],
  }
}
