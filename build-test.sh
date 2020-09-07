for TEST in tests/*; do
     (cd $TEST; ./build.sh) || exit 1
done
