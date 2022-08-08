# Commits

* Use [conventional commits](https://www.conventionalcommits.org/en/v1.0.0-beta.4/)
* we're using [standard-version](https://www.npmjs.com/package/standard-version) with some hooks for releases/tags/semver/changelog


## Example Commit Messages

`feat(iOS, Android): Added cool new feature` - (triggers minor version bump & changelog)

`fix(iOS): fixed some bug` - (triggers patch version bump & changelog)

```
feat(iOS): Did A Cool Thing

BREAKING CHANGE: Cool thing breaks ability to do anything else
```
(triggers major version bump & changelog)


# Releasing

* `npm run release` - do a production release ex. 3.2.0 (version, docs, changelog will all be generated from commits)
* `npm run pre-release do a prerelease ex. 3.2.0-0 (this skips changelog, docs, and tag so that patches/minor bumps get all the changelog info)
