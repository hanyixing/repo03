'use strict'
module.exports = {
  NODE_ENV: '"production"',
  VUE_APP_API_BASE_URL: JSON.stringify(process.env.VUE_APP_API_BASE_URL || ''),
  VUE_APP_TITLE: '"V部落"',
  VUE_APP_DEBUG: 'false'
}
