const { defineConfig } = require("eslint/config");
const expo = require("eslint-config-expo/flat");

module.exports = defineConfig([
  expo,
  { ignores: ["dist/**", "coverage/**"] },
  {
    files: ["**/*.ts", "**/*.tsx"],
    rules: {
      "@typescript-eslint/no-explicit-any": "error",
      "@typescript-eslint/consistent-type-imports": "error",
    },
  },
  {
    files: [
      "app/**/*.tsx",
      "src/components/**/*.tsx",
      "src/features/**/*screen.tsx",
    ],
    rules: {
      "no-restricted-imports": [
        "error",
        { paths: [{ name: "react", importNames: ["useEffect"] }] },
      ],
    },
  },
]);
