use rand::RngExt;

/// No 0/O, 1/I/L — players type these by hand in chat.
pub const ALPHABET: &[u8] = b"23456789ABCDEFGHJKMNPQRSTUVWXYZ";
pub const CODE_LEN: usize = 8;

pub fn new_code() -> String {
    let mut rng = rand::rng();
    (0..CODE_LEN)
        .map(|_| ALPHABET[rng.random_range(0..ALPHABET.len())] as char)
        .collect()
}

pub fn normalize(code: &str) -> String {
    code.trim().to_uppercase()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn codes_use_only_the_unambiguous_alphabet() {
        for _ in 0..200 {
            let c = new_code();
            assert_eq!(c.len(), CODE_LEN);
            assert!(c.bytes().all(|b| ALPHABET.contains(&b)), "{c}");
        }
    }

    #[test]
    fn normalize_trims_and_uppercases() {
        assert_eq!(normalize("  ab3k9mzq \n"), "AB3K9MZQ");
    }
}
