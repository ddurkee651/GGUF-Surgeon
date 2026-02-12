import sys
import json
import os
from pathlib import Path

# Add gguf package to path
sys.path.append(str(Path(__file__).parent))

from gguf import GGUFReader, GGUFWriter
from gguf import GGUFValueType

class GGUFSurgeon:
    """Pure Python GGUF model surgeon - NO COMPILATION NEEDED!"""
    
    @staticmethod
    def inspect_model(model_path):
        """Extract all metadata and tensor info"""
        reader = GGUFReader(model_path)
        
        # Get all metadata
        metadata = {}
        for key in reader.fields:
            field = reader.fields[key]
            metadata[key] = str(field.value)
        
        # Get tensor info
        tensors = []
        for tensor in reader.tensors:
            tensors.append({
                'name': tensor.name,
                'shape': tensor.shape.tolist(),
                'type': str(tensor.tensor_type),
                'bytes': len(tensor.data)
            })
        
        # Get model properties
        architecture = metadata.get('general.architecture', 'unknown')
        context_length = int(metadata.get('llama.context_length', 4096))
        rope_base = float(metadata.get('rope.freq_base', 10000.0))
        rope_scaling = float(metadata.get('rope.scaling.linear', 1.0))
        
        return {
            'name': os.path.basename(model_path).replace('.gguf', ''),
            'architecture': architecture,
            'context_length': context_length,
            'rope_base': rope_base,
            'rope_scaling': rope_scaling,
            'quantization': metadata.get('general.file_type', 'unknown'),
            'tensor_count': len(tensors),
            'tokenizer': metadata.get('tokenizer.ggml.model', 'unknown'),
            'metadata': metadata,
            'tensors': tensors,
            'file_size': os.path.getsize(model_path)
        }
    
    @staticmethod
    def edit_metadata(input_path, output_path, updates):
        """Edit metadata and save new GGUF file"""
        reader = GGUFReader(input_path)
        writer = GGUFWriter(output_path, reader.architecture)
        
        # Copy all metadata, updating specified keys
        for key in reader.fields:
            field = reader.fields[key]
            if key in updates:
                writer.add_string(key, updates[key])
            else:
                # Copy original value with correct type
                if field.types == [GGUFValueType.STRING]:
                    writer.add_string(key, str(field.value))
                elif field.types == [GGUFValueType.INT32]:
                    writer.add_int(key, int(field.value))
                elif field.types == [GGUFValueType.FLOAT32]:
                    writer.add_float(key, float(field.value))
                elif field.types == [GGUFValueType.BOOL]:
                    writer.add_bool(key, bool(field.value))
        
        # Copy all tensors (unchanged)
        for tensor in reader.tensors:
            writer.add_tensor(
                name=tensor.name,
                tensor=tensor.data,
                raw_shape=tensor.shape
            )
        
        writer.close()
        return {'output': output_path}
    
    @staticmethod
    def merge_lora(base_path, lora_path, alpha, output_path):
        """Merge LoRA weights into base model"""
        # This requires numpy for tensor operations
        import numpy as np
        
        base = GGUFReader(base_path)
        lora = GGUFReader(lora_path)
        
        writer = GGUFWriter(output_path, base.architecture)
        
        # Copy base metadata
        for key in base.fields:
            field = base.fields[key]
            if field.types == [GGUFValueType.STRING]:
                writer.add_string(key, str(field.value))
            elif field.types == [GGUFValueType.INT32]:
                writer.add_int(key, int(field.value))
            elif field.types == [GGUFValueType.FLOAT32]:
                writer.add_float(key, float(field.value))
        
        # Merge tensors
        base_tensors = {t.name: t for t in base.tensors}
        lora_tensors = {t.name: t for t in lora.tensors}
        
        for name, base_tensor in base_tensors.items():
            if name in lora_tensors:
                # Apply LoRA weights
                merged = base_tensor.data + (alpha * lora_tensors[name].data)
                writer.add_tensor(name, merged, base_tensor.shape)
            else:
                # Keep original
                writer.add_tensor(name, base_tensor.data, base_tensor.shape)
        
        writer.close()
        return {'output': output_path}

# JNI-like interface for Android
def process_command(command, params):
    surgeon = GGUFSurgeon()
    
    if command == 'inspect':
        return surgeon.inspect_model(params['path'])
    elif command == 'edit':
        return surgeon.edit_metadata(params['input'], params['output'], params['updates'])
    elif command == 'merge':
        return surgeon.merge_lora(params['base'], params['lora'], params['alpha'], params['output'])
    elif command == 'quantize':
        # Quantization requires llama.cpp - return not implemented
        return {'error': 'Quantization requires llama.cpp binary'}
    else:
        return {'error': 'Unknown command'}
