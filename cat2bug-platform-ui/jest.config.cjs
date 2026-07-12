module.exports = {
  testMatch: ['**/tests/unit/**/*.spec.js'],
  moduleNameMapper: {
    '^@/stores/permission$': '<rootDir>/tests/mocks/stores-permission.cjs',
    '^@/(.*)$': '<rootDir>/src/$1',
    '^vue-i18n$': '<rootDir>/node_modules/vue-i18n/dist/vue-i18n.cjs',
    '^element-plus$': '<rootDir>/tests/mocks/element-plus.cjs',
    '^element-plus/es/locale/lang/(.*)$': '<rootDir>/tests/mocks/element-plus-locale.cjs'
  },
  transformIgnorePatterns: [
    '/node_modules/(?!(vue-i18n|@intlify)/)'
  ],
  transform: {
    '^.+\\.js$': 'babel-jest'
  },
  testEnvironment: 'node'
}
