module.exports = {
  testMatch: ['**/tests/unit/**/*.spec.js'],
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1'
  },
  transform: {
    '^.+\\.js$': 'babel-jest'
  },
  testEnvironment: 'node'
}
