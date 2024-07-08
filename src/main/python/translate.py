import warnings
warnings.simplefilter(action='ignore', category=FutureWarning)
import io
import sys
from transformers import MarianMTModel, MarianTokenizer

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

def translate_text(text, src_lang, tgt_lang):
    try:
        model_name = f'Helsinki-NLP/opus-mt-{src_lang}-{tgt_lang}'
        tokenizer = MarianTokenizer.from_pretrained(model_name)
        model = MarianMTModel.from_pretrained(model_name)

        # Tokenize input text
        tokens = tokenizer(text, return_tensors="pt", truncation=True, padding=True)

        # Perform translation
        translated_tokens = model.generate(**tokens)
        translated_text = tokenizer.decode(translated_tokens[0], skip_special_tokens=True)

        return translated_text
    except Exception as e:
        print(f"Error: {str(e)}", file=sys.stderr)
        return f"Error: {str(e)}"

if __name__ == "__main__":
    try:
        text_to_translate = sys.argv[1]
        src_lang = sys.argv[2]
        tgt_lang = sys.argv[3]
        translated_text = translate_text(text_to_translate, src_lang, tgt_lang)
        print(translated_text)
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)