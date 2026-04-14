import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';

const defaultApiBaseUrl = 'http://localhost:8080';
const configuredApiBaseUrl = (process.env.QUIZMI_API_BASE_URL || defaultApiBaseUrl).trim().replace(/\/+$/, '');
const outputPath = resolve('src/app/core/config/generated-app-config.ts');

mkdirSync(dirname(outputPath), { recursive: true });
writeFileSync(
  outputPath,
  `export const generatedAppConfig = {
  apiBaseUrl: ${JSON.stringify(configuredApiBaseUrl || defaultApiBaseUrl)}
} as const;
`
);
