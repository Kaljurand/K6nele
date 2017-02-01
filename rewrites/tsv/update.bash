#!/bin/bash

# Downloads a Google Sheet as TSV, removes the DOS line endings and trailing tabs.
function update {
    echo "$1"
    wget -O - "https://docs.google.com/spreadsheets/d/$2/export?format=tsv" | tr -d '\015' | sed 's/	*$//' > $3.tsv
}

update Map 1liMiWDiU4iN1faAENtAIbFenbtpjKocJvNxjyuW9hqU k6_skill_map
update Various 1SXxXcJf6YQv7ALb_2QJWPs9tVsk4SGc-vxSy6n6l1S0 k6_various
update Translate 1ndVmgLCG1wZ0cedfaAhL_kzw9aoqyP5jnsp1I-qFHwQ k6_skill_translate
update Send 1a_waZskhCxM0NGy6T0_cIAzWd7rHocg0kBvFAIJ6M2s k6_skill_send
