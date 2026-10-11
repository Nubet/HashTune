import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import sharp from "sharp";

for (const color of ["blue", "white"]) {
  const source = await readFile(
    new URL(
      `../../frontend/public/hashtune-logo-${color}.svg`,
      import.meta.url,
    ),
  );
  await sharp(source)
    .resize(512, 512)
    .png()
    .toFile(
      fileURLToPath(new URL(`../assets/logo-${color}.png`, import.meta.url)),
    );
}

const logo = await sharp(
  fileURLToPath(new URL("../assets/logo-blue.png", import.meta.url)),
)
  .resize(600, 600)
  .toBuffer();

for (const [name, background] of [
  ["icon", "#F7F8FB"],
  ["adaptive-icon", "#00000000"],
]) {
  const target = fileURLToPath(
    new URL(`../assets/${name}.png`, import.meta.url),
  );
  await sharp({
    create: { width: 1024, height: 1024, channels: 4, background },
  })
    .composite([{ input: logo, gravity: "centre" }])
    .png()
    .toFile(target);
  assert.equal((await sharp(target).metadata()).width, 1024);
}
