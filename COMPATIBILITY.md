# Frontend Compatibility Notes

This repo is being matched against the existing JS frontend in
`/root/.openclaw/workspace/personal-space-java-sandbox`.

## Checked in this pass

- `public/app.js` main/dynamic page
  - login, register, logout, reset-password, profile update
  - dynamic list/detail, likes, comments, visitors, notifications
  - announcements area embedded in the main page
- `public/article.js` blog / chitchat page
  - article list/detail
  - blog/chitchat publish + edit + delete flows
  - editor image upload contract
- `public/announcements.html`
  - announcement list/detail
  - publish, delete, pin flows
- `public/detail.html`
  - standalone post detail + view count
- superadmin panel inside `public/app.js`
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
- Auth flows now follow the JS backend throttling rules
  - `POST /api/register`: 5 requests per minute per client IP
  - `POST /api/login`: 10 requests per minute per client IP

## Fixed in this pass

- Announcement list loading now matches the JS frontend's no-pagination pages
  - `GET /api/announcements` returns the full list when the frontend omits
    `page` and `limit`
  - explicit paginated callers can still send `page` / `limit` and keep the
    existing paginated response shape
  - this prevents older announcements from disappearing from
    `public/app.js` and `public/announcements.html`
- Auth throttling is now aligned with the JS backend
  - repeated login attempts now return
    `登录尝试过于频繁，请稍后再试`
  - repeated register attempts now return
    `注册请求过于频繁，请稍后再试`
  - this closes the last obvious auth-page behavior gap found in the frontend
    compatibility review
- Main feed post deletion now follows the current `public/app.js` UI rule
  - `DELETE /api/posts/:id` allows the post owner even if that user no longer
    has the `admin` role
  - superadmins can still delete any post
  - this matches the delete button logic shown in the main feed cards

## Remaining small differences

- Some non-critical error text may still differ from the original Node routes
- The Java backend is slightly stricter in a few validations and cleanup paths
- Static HTML route hosting still belongs to the JS frontend/proxy setup;
  this Java backend is focused on API compatibility
