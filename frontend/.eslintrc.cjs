/**
 * ESLint configuration.
 *
 * The rule worth understanding here is react-hooks/exhaustive-deps. It warns when a useEffect
 * reads a value that is not in its dependency array - the cause of "stale closure" bugs where
 * an effect keeps using the first render's value forever. When you genuinely need to ignore it
 * (as in useApiData, where adding the fetcher would loop), silence that ONE line with a
 * comment explaining why, rather than switching the rule off globally.
 *
 * The file is .cjs because package.json sets "type": "module", which would otherwise make
 * Node treat this as an ES module and reject module.exports.
 */
module.exports = {
  root: true,
  env: { browser: true, es2022: true },
  extends: [
    'eslint:recommended',
    'plugin:react/recommended',
    'plugin:react/jsx-runtime',
    'plugin:react-hooks/recommended',
  ],
  ignorePatterns: ['dist', 'node_modules', '.eslintrc.cjs'],
  parserOptions: { ecmaVersion: 'latest', sourceType: 'module' },
  settings: { react: { version: '18.3' } },
  plugins: ['react-refresh'],
  rules: {
    // React 18 + the new JSX runtime means `import React from 'react'` is not needed in every
    // file, so these two legacy rules are off.
    'react/react-in-jsx-scope': 'off',
    'react/prop-types': 'off',
    'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
    'no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
  },
};
