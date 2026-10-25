<?php

namespace App\Http\Controllers;

use App\Models\Post;
use App\Models\Project;
use App\Models\Service;
use App\Models\Setting;
use App\Models\Tool;
use App\Support\Booking;
use App\Support\FreeTools;
use Illuminate\Support\Facades\Cache;

/**
 * Everything the Bytebench Android app shows, as one JSON file:
 *   GET /api/app/content.json
 *
 * The shape matches app/src/main/assets/content.json in the app, so whatever you
 * publish in the admin panel appears in the app after it syncs. Only published,
 * public content is included — no messages, requests, subscribers or settings secrets.
 */
class AppContentController extends Controller
{
    public function __invoke()
    {
        // Rebuilt at most every 10 minutes; edits in the admin panel show up after that.
        $json = Cache::remember('app_content_json', 600, fn () => $this->build());

        return response()->json($json, 200, [
            'Cache-Control' => 'public, max-age=300',
        ], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    }

    private function build(): array
    {
        $site = config('site');
        $free = config('freetools');
        $o = $site['owner'];
        $lines = fn ($s) => array_values(array_filter(array_map('trim', preg_split('/\r?\n/', $s ?? ''))));

        $tools = FreeTools::visible()->map(function ($t) use ($free) {
            $pitch = $free['tool_pitches'][$t->slug] ?? $free['pitches'][$t->category] ?? null;

            return [
                'slug' => $t->slug, 'name' => $t->name, 'category' => $t->category, 'icon' => $t->icon,
                'summary' => $t->summary, 'steps' => $t->steps ?? [], 'about' => $t->about ?? '',
                'faqs' => array_map(fn ($f) => ['q' => $f[0], 'a' => $f[1]], $t->faqs ?? []),
                'pitch' => $pitch, 'topics' => FreeTools::topics($t),
            ];
        })->values();

        $featured = FreeTools::featured()->pluck('slug')->all() ?: $free['default_featured'];

        $services = Service::published()->get()->map(fn (Service $s) => [
            'slug' => $s->slug, 'title' => $s->title, 'icon' => $s->icon_class, 'summary' => $s->summary,
            'includes' => $s->include_list, 'details' => $s->details ?? '', 'priceLabel' => $s->price_label,
            'sortOrder' => $s->sort_order,
        ]);

        $projects = Project::where('is_published', true)->orderBy('sort_order')->get()->map(fn (Project $p) => [
            'slug' => $p->slug, 'title' => $p->title, 'summary' => $p->summary ?? '', 'role' => $p->role, 'year' => $p->year,
            'tech' => $p->tech_list, 'url' => $p->url, 'image' => $p->image ? 'storage/'.$p->image : null,
            'challenge' => $p->challenge ?? '', 'approach' => $p->approach ?? '', 'outcome' => $p->outcome ?? '',
        ]);

        $posts = Post::published()->take(100)->get()->map(fn (Post $p) => [
            'slug' => $p->slug, 'title' => $p->title, 'category' => $p->category, 'excerpt' => $p->summary,
            'body' => $p->body, 'cover' => $p->cover_image ? 'storage/'.$p->cover_image : null,
            'publishedAt' => optional($p->published_at ?? $p->created_at)->toDateString(),
            'readingMinutes' => $p->reading_minutes,
        ]);

        $products = Tool::published()->get()->map(function (Tool $t) use ($lines) {
            $url = $t->external_url
                ?: ($t->isFree() && $t->hasBuiltinApp() ? route('tools.run', $t)
                : ($t->isFree() && $t->file_path ? route('tools.download', $t) : route('tools.show', $t)));

            return [
                'slug' => $t->slug, 'name' => $t->name, 'tagline' => $t->tagline ?? '', 'description' => $t->description ?? '',
                'type' => $t->type, 'pricing' => $t->pricing, 'priceLabel' => $t->price_label, 'version' => $t->version,
                'cover' => $t->cover_image ? 'storage/'.$t->cover_image : null, 'features' => $lines($t->features),
                'url' => $url,
            ];
        });

        $legal = collect(LegalController::PAGES)->map(fn ($title, $slug) => [
            'slug' => $slug, 'title' => $title,
            'body' => self::htmlToMarkdown(view('legal.'.$slug, ['title' => $title, 'updated' => $site['legal_updated']])->renderSections()['legal'] ?? ''),
        ])->values();

        $socials = collect($site['socials'])->filter()->map(fn ($url, $name) => [
            'name' => $name, 'url' => $url, 'icon' => $site['social_icons'][$name] ?? 'fa-solid fa-link',
        ])->values();

        $booking = $site['booking'];
        $booking['days'] = Booking::days();
        $booking['from'] = Booking::from();
        $booking['to'] = Booking::to();

        return [
            'version' => 1,
            'generatedAt' => now()->toIso8601String(),
            'site' => [
                'name' => $site['name'], 'tagline' => $site['tagline'], 'description' => $site['description'],
                'owner' => [
                    'name' => $o['name'], 'shortName' => $o['short_name'], 'role' => $o['role'], 'location' => $o['location'],
                    'email' => $o['email'], 'whatsapp' => $o['whatsapp'], 'whatsappDisplay' => $o['whatsapp_display'],
                    'whatsappMessage' => $o['whatsapp_message'],
                ],
                'socials' => $socials,
                'intro' => $site['intro'],
                'experience' => $site['experience'],
                'skills' => collect($site['skills'])->map(fn ($items, $group) => ['group' => $group, 'items' => $items])->values(),
                'education' => $site['education'],
                'stats' => array_map(fn ($s) => [
                    'label' => $s['label'], 'since' => $s['since'] ?? null, 'value' => $s['value'] ?? null,
                    'count' => $s['count'] ?? null, 'suffix' => $s['suffix'] ?? '',
                ], $site['stats']),
                'steps' => $site['steps'],
                'booking' => $booking,
                'authorBio' => $site['author_bio'],
                'blogCategories' => $site['blog_categories'],
                'legalUpdated' => $site['legal_updated'],
                'defaultSkin' => Setting::get('skin', $site['skin']),
            ],
            'toolCategories' => collect($free['categories'])->map(fn ($name, $slug) => ['slug' => $slug, 'name' => $name])->values(),
            'featuredTools' => $featured,
            'tools' => $tools,
            'services' => $services,
            'projects' => $projects,
            'posts' => $posts,
            'products' => $products,
            'legal' => $legal,
        ];
    }

    /** The legal pages are written in simple HTML; the app renders Markdown. */
    private static function htmlToMarkdown(string $html): string
    {
        $md = preg_replace_callback('#<a [^>]*href="([^"]*)"[^>]*>(.*?)</a>#s', fn ($m) => $m[1] === strip_tags($m[2]) ? $m[1] : '['.strip_tags($m[2]).']('.$m[1].')', $html);
        $md = preg_replace('#<(strong|b)>(.*?)</\1>#s', '**$2**', $md);
        $md = preg_replace('#<(em|i)>(.*?)</\1>#s', '*$2*', $md);
        $md = preg_replace('#<h2[^>]*>(.*?)</h2>#s', "\n\n## $1\n\n", $md);
        $md = preg_replace('#<h3[^>]*>(.*?)</h3>#s', "\n\n### $1\n\n", $md);
        $md = preg_replace('#<li[^>]*>(.*?)</li>#s', "\n- $1", $md);
        $md = preg_replace('#</?(ul|ol)[^>]*>#', "\n", $md);
        $md = preg_replace('#<p[^>]*>(.*?)</p>#s', "\n\n$1\n\n", $md);
        $md = preg_replace('#<br\s*/?>#', "\n", $md);
        $md = html_entity_decode(strip_tags($md), ENT_QUOTES | ENT_HTML5, 'UTF-8');
        $md = preg_replace("/[ \t]+\n/", "\n", $md);

        return trim(preg_replace("/\n{3,}/", "\n\n", $md));
    }
}
