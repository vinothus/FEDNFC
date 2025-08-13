-- Check the raw extracted text in database for invoice ID 26
-- Run this after scheduler processes your SuperStore invoice

-- 1. Quick overview of latest invoices
SELECT 
    id,
    original_filename,
    ocr_status,
    ocr_method,
    ocr_confidence,
    LENGTH(raw_extracted_text) as text_length,
    text_word_count,
    created_at
FROM invoices 
WHERE id >= 26
ORDER BY id DESC;

-- 2. Get the actual raw text for SuperStore invoice (ID 26)
SELECT 
    id,
    original_filename,
    ocr_method,
    ocr_confidence,
    LENGTH(raw_extracted_text) as full_length,
    raw_extracted_text
FROM invoices 
WHERE id = 26;

-- 3. Check for alignment characters in the text
SELECT 
    id,
    original_filename,
    CASE 
        WHEN raw_extracted_text LIKE '%	%' THEN 'HAS_TABS'
        WHEN raw_extracted_text LIKE '%  %' THEN 'HAS_MULTIPLE_SPACES' 
        ELSE 'NO_ALIGNMENT_CHARS'
    END as alignment_check,
    LENGTH(raw_extracted_text) as text_length,
    SUBSTRING(raw_extracted_text, 1, 300) as first_300_chars
FROM invoices 
WHERE id = 26;
