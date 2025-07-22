# AI Models

The following files must be downloaded manually and placed in this folder:

## Required Models

### Gemma 3n Model
- **File**: `gemma3n/gemma-3n-E2B-it-int4.task`
- **Size**: ~3.1 GB
- **Purpose**: Main language model for SMS classification
- **Source**: Google AI Edge (Gemma 3n model)

### Embedding Models
- **File**: `embeddings/sms_embedding_model.tflite`
- **Size**: ~1.4 MB
- **Purpose**: TensorFlow Lite model for SMS embeddings

### Embedding Data
- **File**: `embeddings/benign_embeddings.json`
- **Size**: ~10.7 MB
- **Purpose**: Pre-computed embeddings for benign SMS examples

- **File**: `embeddings/smishing_embeddings.json`
- **Size**: ~10.8 MB
- **Purpose**: Pre-computed embeddings for smishing SMS examples

## Directory Structure

```
app/src/main/assets/models/
├── gemma3n/
│   └── gemma-3n-E2B-it-int4.task
├── embeddings/
│   ├── sms_embedding_model.tflite
│   ├── benign_embeddings.json
│   ├── smishing_embeddings.json
│   ├── embeddings_metadata.json
│   ├── prompt.md
│   └── README.md
└── README.md (this file)
```

## Notes

- These files are **NOT** included in the git repository due to their large size
- The `.gitignore` file is configured to exclude these model files
- Make sure to download and place these files before building the app
- The app will fail to run if these models are missing

## How to Obtain Models

1. **Gemma 3n Model**: Download from Google AI Edge or your model provider
2. **Embedding Models**: Generate using your training pipeline or download from your model repository
3. **Embedding Data**: Generate embeddings for your SMS dataset using the embedding model

## Development Setup

For development, you may need to:
1. Download the models from your team's shared storage
2. Place them in the correct directories as shown above
3. Ensure the file names match exactly
4. Build and run the app

## Troubleshooting

If the app crashes on startup, check that:
- All model files are present in the correct locations
- File names match exactly (case-sensitive)
- Files are not corrupted during download 