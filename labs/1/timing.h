#define START_TIMING(s) auto begin_##s = std::chrono::steady_clock::now();
#define STOP_TIMING(s, desc) \
  auto end_##s = std::chrono::steady_clock::now(); \
  printf("%s took %ldus\n", desc, \
    std::chrono::duration_cast<std::chrono::microseconds>(end_##s - begin_##s).count());