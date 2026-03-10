# Frontend Compatibility Notes

This repo is being matched against the existing JS frontend in
`/root/.openclaw/workspace/personal-space-java-sandbox`.

## Checked in this pass

- `public/app.js` main/dynamic page
  - login state via `/api/me`
  - dynamic list/detail, likes, comments, visitors, notifications
  - announcements area embedded in the main page
- `public/article.js` blog / chitchat page
  - article list/detail
  - blog/chitchat publish + edit + delete flows
  - editor image upload contract
- superadmin panel
  - `/api/users`
  - role update
  - invite code fetch/refresh
  - reset code fetch/create
  - delete user

## Already aligned

- Main feed/article/announcement payloads use the JS frontend field names:
  `created_at`, `author_name`, `author_avatar`, `like_count`,
  `comment_count`, `cover_image`, `parent_id`, `reply_to_nickname`
- Main list APIs return the expected pagination shape:
  `{ page, limit, total, pages }`
- Article upload endpoint keeps the editor-friendly response shape:
  `{ code, msg, data: { errFiles, succMap } }`
- Superadmin-only endpoints are present for users, invite codes, reset codes,
  visitors, and announcements

## Fixed in this pass

- Invite code day handling now follows the JS backend rule consistently:
  use the UTC calendar day everywhere
  - this avoids midnight mismatches between
    `GET /api/invite-code`,
    `POST /api/invite-code/refresh`,
    startup invite-code creation,
    and `POST /api/register`
- SQLite foreign keys are now explicitly enabled for Java connections
  - this makes delete cascades behave like the schema intends
  - it especially helps superadmin cleanup flows stay consistent after deleting
    posts/users

## Remaining small differences

- Some non-critical error text may still differ from the original Node routes
- The Java backend is slightly stricter in a few validations and cleanup paths,
  but the current JS frontend pages checked in this pass should continue to work
