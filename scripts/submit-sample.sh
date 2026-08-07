#!/usr/bin/env bash
#
# Posts the sample pacs.008 to a running service and prints the response.
#
# The sample settles on 2026-07-31, which is in the past, and the settlement date rule
# rejects backdated payments. Rather than weaken the rule or ship a sample that stops
# working, this script stamps today's date before posting.
#
# Usage:
#   scripts/submit-sample.sh                      accept path, fresh transaction id
#   scripts/submit-sample.sh --duplicate          reuse the last transaction id
#   scripts/submit-sample.sh --replay             reuse the last idempotency key
#   scripts/submit-sample.sh --reject-currency    change BBD to USD
#   scripts/submit-sample.sh --reject-parties     make sender and receiver identical
#   scripts/submit-sample.sh --original           post the supplied sample unmodified

set -euo pipefail

URL="${PAYMENT_SERVICE_URL:-http://localhost:8080/api/v1/payments}"
SAMPLE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/src/test/resources/samples"
SAMPLE="${SAMPLE_DIR}/pacs008-valid.xml"
TODAY="$(date +%Y-%m-%d)"
REQUEST="$(mktemp)"
trap 'rm -f "${REQUEST}"' EXIT

MODE="${1:---accept}"

if [[ "${MODE}" == "--original" ]]; then
  # The sample exactly as supplied. Expect 422: its accounts carry no institution prefix
  # and its transit numbers are five digits where the brief specifies three.
  SAMPLE="${SAMPLE_DIR}/pacs008-sample.xml"
fi

sed "s|<IntrBkSttlmDt>2026-07-31</IntrBkSttlmDt>|<IntrBkSttlmDt>${TODAY}</IntrBkSttlmDt>|" \
  "${SAMPLE}" > "${REQUEST}"

# A fresh transaction id and idempotency key each run, so repeated runs exercise the accept
# path rather than tripping duplicate detection. --duplicate and --replay deliberately fix
# one or both.
#
# The process id and $RANDOM matter: a timestamp alone has one-second resolution, so two
# runs in the same second would collide and the second would be refused as a reused key.
UNIQUE="$(date +%Y%m%d%H%M%S)-$$-${RANDOM}"
TRANSACTION_ID="TX-${UNIQUE}"
IDEMPOTENCY_KEY="KEY-${UNIQUE}"

case "${MODE}" in
  --duplicate)
    TRANSACTION_ID="TX-DUPLICATE-FIXED"
    ;;
  --replay)
    TRANSACTION_ID="TX-REPLAY-FIXED"
    IDEMPOTENCY_KEY="KEY-REPLAY-FIXED"
    ;;
  --reject-currency)
    sed -i.bak 's|Ccy="BBD"|Ccy="USD"|g' "${REQUEST}" && rm -f "${REQUEST}.bak"
    ;;
  --reject-parties)
    sed -i.bak 's|<BICFI>CBANK0IPS</BICFI>|<BICFI>BANKA000</BICFI>|' "${REQUEST}" \
      && rm -f "${REQUEST}.bak"
    ;;
esac

if [[ "${MODE}" != "--original" ]]; then
  sed -i.bak "s|<TxId>B621200494113</TxId>|<TxId>${TRANSACTION_ID}</TxId>|" "${REQUEST}" \
    && rm -f "${REQUEST}.bak"
fi

echo "POST ${URL}"
echo "  settlement date  ${TODAY}"
echo "  transaction id   ${TRANSACTION_ID}"
echo "  idempotency key  ${IDEMPOTENCY_KEY}"
echo

curl --silent --show-error --include \
  --request POST "${URL}" \
  --header 'Content-Type: application/xml' \
  --header "X-Idempotency-Key: ${IDEMPOTENCY_KEY}" \
  --header 'X-Sender-Institution: BANKA000' \
  --data-binary "@${REQUEST}"

echo
