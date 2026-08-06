# Manage images by S3 object key

Store stable S3 object keys, rather than public URLs or image row IDs, in business image fields. Browser uploads use short-lived presigned PUT requests and become Managed Images only after backend metadata verification; public URLs are derived from configured `publicBaseUrl`, so storage presentation can change without rewriting business rows. Image records intentionally have no uploader ownership, and administrator deletion does not rewrite business references, accepting controllable broken-reference risk in exchange for a simple shared image lifecycle.
