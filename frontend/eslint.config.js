import js from '@eslint/js';
import prettier from 'eslint-config-prettier';
import vue from 'eslint-plugin-vue';
import vueParser from 'vue-eslint-parser';
import tseslint from 'typescript-eslint';

export default tseslint.config(
  {
    ignores: ['dist/**', 'coverage/**', 'node_modules/**'],
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...vue.configs['flat/recommended'],
  {
    files: ['**/*.vue'],
    languageOptions: {
      parser: vueParser,
      globals: {
        clearTimeout: 'readonly',
        console: 'readonly',
        document: 'readonly',
        localStorage: 'readonly',
        navigator: 'readonly',
        setTimeout: 'readonly',
        URL: 'readonly',
        window: 'readonly',
      },
      parserOptions: {
        parser: tseslint.parser,
        ecmaVersion: 'latest',
        sourceType: 'module',
        extraFileExtensions: ['.vue'],
      },
    },
    rules: {
      'vue/attributes-order': 'off',
      'vue/multi-word-component-names': 'off',
    },
  },
  {
    files: ['**/*.{ts,vue,js,mjs}'],
    languageOptions: {
      globals: {
        clearTimeout: 'readonly',
        console: 'readonly',
        document: 'readonly',
        fetch: 'readonly',
        localStorage: 'readonly',
        navigator: 'readonly',
        process: 'readonly',
        setTimeout: 'readonly',
        URL: 'readonly',
        window: 'readonly',
      },
    },
    rules: {
      '@typescript-eslint/no-unused-vars': [
        'error',
        {
          argsIgnorePattern: '^_',
          varsIgnorePattern: '^_',
        },
      ],
    },
  },
  prettier,
);
