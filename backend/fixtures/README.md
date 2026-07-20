`results_bs6stem/03/{vocals,drums,bass,guitar,piano,other}.wav` are **real BS-Roformer output**,
generated 2026-07-14 by a real forward pass on a100server2 (GPU 1, A100-40GB) against
`test_tracks/03.flac` (4:07). 33.04s wall clock for both `03` and `03(1)` combined —
consistent with the doc's ~11x-realtime figure. `results_bs6stem/03(1)/` holds the second
track's real output for the same reason.

Command used (from `~/stem-separation-validation/msst` on the server):
```
../.venv-msst/bin/python3 inference.py \
  --model_type bs_roformer \
  --config_path ckpts/bs_6stem_config.yaml \
  --start_check_point ckpts/bs_6stem.ckpt \
  --input_folder <uploaded tracks dir> \
  --store_dir <output dir> \
  --device_ids 1 --pcm_type FLOAT
```

To regenerate with a different input track, scp it to the server, rerun, and scp the six
`*.wav` files back into this directory — same filenames, no code changes needed.
