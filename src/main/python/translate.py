import warnings
warnings.simplefilter(action='ignore', category=FutureWarning)

import sys
from transformers import MarianMTModel, MarianTokenizer

def translate_text(text, src_lang="en", tgt_lang="de"):
    try:
        model_name = f'Helsinki-NLP/opus-mt-{src_lang}-{tgt_lang}'
        tokenizer = MarianTokenizer.from_pretrained(model_name)
        model = MarianMTModel.from_pretrained(model_name)

        # Tokenize input text
        tokens = tokenizer.prepare_seq2seq_batch([text], return_tensors="pt")

        # Perform translation
        translated_tokens = model.generate(**tokens)
        translated_text = tokenizer.decode(translated_tokens[0], skip_special_tokens=True)

        return translated_text
    except Exception as e:
        return str(e)

if __name__ == "__main__":
    try:
        text_to_translate = sys.argv[1]
        translated_text = translate_text(text_to_translate)
        print(translated_text)
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
