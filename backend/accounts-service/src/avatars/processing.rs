use image::{ImageFormat, ImageReader, Limits, imageops::FilterType};
use std::io::Cursor;

pub const AVATAR_SIZE: u32 = 256;
pub const MAX_DIMENSION: u32 = 8192;
const MAX_DECODE_BYTES: u64 = 128 * 1024 * 1024;

#[derive(Debug, PartialEq, Eq)]
pub enum AvatarImageError {
    /// Not a PNG/JPEG/WebP at all.
    Unsupported,
    /// Claims to be one of those but does not decode within the limits.
    Invalid,
}

/// Decodes untrusted image bytes, center-crops to a square and resizes to
/// `AVATAR_SIZE`x`AVATAR_SIZE`, then re-encodes as PNG. The output is always
/// a freshly generated PNG — never the original bytes — so EXIF metadata and
/// any embedded payload never reach storage.
pub fn process_avatar(bytes: &[u8]) -> Result<Vec<u8>, AvatarImageError> {
    let mut reader = ImageReader::new(Cursor::new(bytes))
        .with_guessed_format()
        .map_err(|_| AvatarImageError::Invalid)?;
    if !matches!(
        reader.format(),
        Some(ImageFormat::Png | ImageFormat::Jpeg | ImageFormat::WebP)
    ) {
        return Err(AvatarImageError::Unsupported);
    }
    let mut limits = Limits::default();
    limits.max_image_width = Some(MAX_DIMENSION);
    limits.max_image_height = Some(MAX_DIMENSION);
    limits.max_alloc = Some(MAX_DECODE_BYTES);
    reader.limits(limits);

    let decoded = reader.decode().map_err(|_| AvatarImageError::Invalid)?;
    let square = decoded.resize_to_fill(AVATAR_SIZE, AVATAR_SIZE, FilterType::Lanczos3);
    let mut out = Cursor::new(Vec::new());
    square
        .write_to(&mut out, ImageFormat::Png)
        .map_err(|_| AvatarImageError::Invalid)?;
    Ok(out.into_inner())
}

#[cfg(test)]
mod tests {
    use super::*;
    use image::ImageFormat;
    use std::io::Cursor;

    fn png_of(width: u32, height: u32) -> Vec<u8> {
        let img = image::RgbaImage::new(width, height);
        let mut out = Cursor::new(Vec::new());
        img.write_to(&mut out, ImageFormat::Png).unwrap();
        out.into_inner()
    }

    #[test]
    fn valid_image_becomes_a_256_square_png() {
        let out = process_avatar(&png_of(300, 100)).unwrap();
        assert!(out.starts_with(&[0x89, b'P', b'N', b'G']));
        let decoded = image::load_from_memory(&out).unwrap();
        assert_eq!(
            (decoded.width(), decoded.height()),
            (AVATAR_SIZE, AVATAR_SIZE)
        );
    }

    #[test]
    fn non_image_bytes_are_unsupported() {
        assert_eq!(
            process_avatar(b"not an image"),
            Err(AvatarImageError::Unsupported)
        );
    }

    #[test]
    fn gif_is_unsupported() {
        assert_eq!(
            process_avatar(b"GIF89a\x01\x00\x01\x00\x00\x00\x00;"),
            Err(AvatarImageError::Unsupported)
        );
    }

    #[test]
    fn truncated_png_is_invalid() {
        let mut bytes = png_of(50, 50);
        bytes.truncate(40);
        assert_eq!(process_avatar(&bytes), Err(AvatarImageError::Invalid));
    }

    #[test]
    fn image_wider_than_the_limit_is_rejected() {
        assert_eq!(
            process_avatar(&png_of(MAX_DIMENSION + 1, 1)),
            Err(AvatarImageError::Invalid)
        );
    }
}
