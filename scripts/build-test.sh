for TEST in tests/*; do
     echo Running: $TEST
     (cd $TEST; ./build.sh) || exit 1
done
