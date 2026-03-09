import nunjucks from "nunjucks";

export class TemplateEngine {
  render(source: string, context: Record<string, unknown>): string {
    return nunjucks.renderString(source, context);
  }
}
