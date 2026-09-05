#!/bin/sh
set -e

# Coze PAT 为空时用占位符（nginx 仍能启动，但 Coze 反代会 401）
COZE_PAT="${COZE_PAT:-__COZE_PAT_NOT_SET__}"

envsubst '${COZE_PAT}' < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf

exec nginx -g 'daemon off;'
