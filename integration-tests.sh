#!/bin/bash
shopt -s extglob
# set -o errexit

function abort {
    if [ $# -gt 0 ]; then
        echo "$*"
    fi
    exit 1
}

function display_help {
    echo "usage: $(basename "$0")"
}

function abort_and_display_help {
    display_help
    abort "$@"
}

asking_for_help=false
case "$1" in
    --help|-h)
        asking_for_help=true
        ;;
esac

here="$(cd -L "$(dirname "$(readlink "$0" || echo "$0")")";pwd)"

if [ $asking_for_help = true ]; then
    display_help
    exit 0
fi

set -o nounset

exec java -cp "$(lein classpath)" clojure.main "$here/integration-tests.clj"



