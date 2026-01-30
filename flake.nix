{
  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";

  outputs = { self, nixpkgs }:
    let
      supportedSystems = [ "x86_64-linux" "x86_64-darwin" "aarch64-linux" "aarch64-darwin" ];
      forAllSystems = nixpkgs.lib.genAttrs supportedSystems;
      pkgs = forAllSystems (system: nixpkgs.legacyPackages.${system});
    in
    {
      packages = forAllSystems (system:
        with pkgs.${system}; {
        docs = (writeShellScriptBin "docs"
          ''${clojure}/bin/clj -X:docs
            echo "js4clj.coruscation.net" > docs/CNAME'');
      });

      devShells = forAllSystems (system: let
      in {
        default = pkgs.${system}.mkShellNoCC {
          packages = with pkgs.${system}; [
            clojure
            gnumake
            emacs
          ];
        };
      });
    };
}
